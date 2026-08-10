package br.rafaeros.fastrelax_api.features.collaborators;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.rafaeros.fastrelax_api.features.settings.SessionSettingsService;
import lombok.RequiredArgsConstructor;

/**
 * Fecha sessões abandonadas.
 *
 * <p>
 * Vive em um bean próprio de propósito: {@link CollaboratorSessionService} chama
 * {@link #expireAbandonedSessions()} antes de cada leitura, e uma chamada a
 * método transacional dentro do mesmo bean não passaria pelo proxy do Spring —
 * a transação seria silenciosamente ignorada.
 */
@Service
@RequiredArgsConstructor
public class SessionExpirationService {

    private static final List<SessionStatus> ACTIVE_STATUSES =
            List.of(SessionStatus.SCHEDULED, SessionStatus.STARTED);

    private final CollaboratorSessionRepository sessionRepository;
    private final SessionSettingsService sessionSettingsService;

    /**
     * Marca como EXPIRED as sessões ativas que ficaram para trás:
     * <ul>
     * <li>SCHEDULED que não foi iniciada até {@code startTime + tolerância};</li>
     * <li>STARTED que não foi finalizada até {@code endTime + tolerância}.</li>
     * </ul>
     * Nos dois casos o horário volta a ficar disponível, e o colaborador deixa de
     * ficar bloqueado pelo índice de sessão ativa única.
     *
     * @return quantas sessões expiraram
     */
    @Transactional
    public int expireAbandonedSessions() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        int graceMinutes = sessionSettingsService.getStartGraceMinutes();

        List<CollaboratorSession> candidates = sessionRepository
                .findByStatusInAndSessionDateLessThanEqual(ACTIVE_STATUSES, today);

        // Loop em vez de peek(): mutar dentro de um stream depende de ele ser
        // consumido, o que torna o efeito colateral frágil e difícil de ler.
        List<CollaboratorSession> expired = new ArrayList<>();
        for (CollaboratorSession session : candidates) {
            if (isAbandoned(session, today, now, graceMinutes)) {
                session.setStatus(SessionStatus.EXPIRED);
                expired.add(session);
            }
        }

        sessionRepository.saveAll(expired);
        return expired.size();
    }

    private boolean isAbandoned(CollaboratorSession session, LocalDate today, LocalTime now, int graceMinutes) {
        // Qualquer sessão ativa de um dia anterior já ficou para trás.
        if (session.getSessionDate().isBefore(today)) {
            return true;
        }
        LocalTime deadline = session.getStatus() == SessionStatus.SCHEDULED
                ? session.getStartTime().plusMinutes(graceMinutes)
                : session.getEndTime().plusMinutes(graceMinutes);

        // plusMinutes vira o dia se o prazo cruzar a meia-noite; aí ainda não venceu.
        if (deadline.isBefore(session.getStartTime())) {
            return false;
        }
        return now.isAfter(deadline);
    }
}
