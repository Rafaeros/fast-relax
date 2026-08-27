package br.rafaeros.fastrelax_api.features.collaborators;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.rafaeros.fastrelax_api.core.tenancy.CurrentTenant;
import br.rafaeros.fastrelax_api.features.settings.SessionSettingsService;
import lombok.RequiredArgsConstructor;

/**
 * Fecha sessões abandonadas da empresa em curso.
 *
 * <p>
 * Vive em um bean próprio de propósito: {@link CollaboratorSessionService} chama
 * {@link #expireAbandonedSessions()} antes de cada leitura, e uma chamada a
 * método transacional dentro do mesmo bean não passaria pelo proxy do Spring —
 * a transação seria silenciosamente ignorada.
 *
 * <p>
 * O escopo é de uma empresa por vez, e não por comodidade: a tolerância de
 * início é configuração de cada cliente, então varrer todo mundo numa consulta
 * só aplicaria o prazo errado a quase todos. Quem precisa passar por todas —
 * o job de fundo — usa {@link SessionExpirationSweeper}.
 */
@Service
@RequiredArgsConstructor
public class SessionExpirationService {

    private static final List<SessionStatus> ACTIVE_STATUSES =
            List.of(SessionStatus.SCHEDULED, SessionStatus.STARTED);

    private final CollaboratorSessionRepository sessionRepository;
    private final SessionSettingsService sessionSettingsService;
    private final br.rafaeros.fastrelax_api.features.chairs.ChairCommandService chairCommandService;
    private final org.springframework.context.ApplicationEventPublisher events;
    private final CurrentTenant currentTenant;

    /**
     * Fecha as sessões ativas que chegaram ao fim, com desfechos diferentes:
     * <ul>
     * <li>SCHEDULED não iniciada até {@code startTime + tolerância} vira EXPIRED
     * — o colaborador não compareceu;</li>
     * <li>STARTED que alcançou o {@code endTime} vira DONE — a sessão rodou até o
     * fim e o relé já desligou sozinho, então ela foi cumprida mesmo sem
     * ninguém apertar "finalizar".</li>
     * </ul>
     * Nos dois casos o horário volta a ficar disponível, e o colaborador deixa de
     * ficar bloqueado pelo índice de sessão ativa única.
     *
     * @return quantas sessões foram encerradas
     */
    @Transactional
    public int expireAbandonedSessions() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        int graceMinutes = sessionSettingsService.getStartGraceMinutes();

        List<CollaboratorSession> candidates = sessionRepository
                .findByCompanyIdAndStatusInAndSessionDateLessThanEqual(currentTenant.companyId(), ACTIVE_STATUSES, today);

        // Loop em vez de peek(): mutar dentro de um stream depende de ele ser
        // consumido, o que torna o efeito colateral frágil e difícil de ler.
        List<CollaboratorSession> closed = new ArrayList<>();
        for (CollaboratorSession session : candidates) {
            if (session.getStatus() == SessionStatus.SCHEDULED) {
                if (missedStart(session, today, now, graceMinutes)) {
                    session.setStatus(SessionStatus.EXPIRED);
                    closed.add(session);
                }
                continue;
            }

            // STARTED que chegou ao fim do horário: a sessão foi cumprida. O relé
            // já desligou sozinho no ESP32 quando a duração acabou, e o comando
            // aqui só cobre o caso de ele ainda estar ligado.
            if (reachedEnd(session, today, now)) {
                chairCommandService.stopFor(session.getChair(), session.getId());
                session.setStatus(SessionStatus.DONE);
                session.setFinishedAt(session.getSessionDate().atTime(session.getEndTime()));
                closed.add(session);
            }
        }

        sessionRepository.saveAll(closed);

        // Publicado só depois do saveAll: o listener roda após o commit, e
        // anunciar antes deixaria o aviso descrever um estado que ainda podia
        // ser desfeito por um erro de gravação.
        for (CollaboratorSession session : closed) {
            events.publishEvent(SessionLifecycleEvent.of(
                    session.getStatus() == SessionStatus.EXPIRED
                            ? SessionLifecycleEvent.Type.EXPIRED
                            : SessionLifecycleEvent.Type.FINISHED,
                    session));
        }

        return closed.size();
    }

    /**
     * Agendou e não apareceu: passou do horário de início mais a tolerância sem
     * ninguém acionar a cadeira.
     */
    private boolean missedStart(CollaboratorSession session, LocalDate today, LocalTime now,
            int graceMinutes) {
        if (session.getSessionDate().isBefore(today)) {
            return true;
        }
        LocalTime deadline = session.getStartTime().plusMinutes(graceMinutes);

        // plusMinutes vira o dia se o prazo cruzar a meia-noite; aí ainda não venceu.
        if (deadline.isBefore(session.getStartTime())) {
            return false;
        }
        return now.isAfter(deadline);
    }

    /**
     * Sessão em andamento que alcançou o fim do horário reservado.
     *
     * <p>
     * Sem tolerância aqui: o relé desliga exatamente no fim da duração, então
     * esticar o prazo só manteria o colaborador bloqueado pelo índice de sessão
     * ativa depois de a cadeira já ter parado.
     */
    private boolean reachedEnd(CollaboratorSession session, LocalDate today, LocalTime now) {
        if (session.getSessionDate().isBefore(today)) {
            return true;
        }
        return !now.isBefore(session.getEndTime());
    }
}
