package br.rafaeros.fastrelax_api.features.notifications;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.rafaeros.fastrelax_api.features.collaborators.CollaboratorSession;
import lombok.RequiredArgsConstructor;

/**
 * Envia uma faixa de lembrete para quem ainda não a recebeu.
 *
 * <p>
 * Vive em um bean separado de {@link SessionReminderJob} pelo mesmo motivo que
 * {@code SessionExpirationService}: chamada a método transacional dentro do
 * próprio bean não passa pelo proxy do Spring, e a transação seria
 * silenciosamente ignorada.
 *
 * <p>
 * A transação importa aqui: a marca é gravada <em>antes</em> do aviso, e as duas
 * coisas precisam cair juntas. Se a notificação falhar, a marca volta atrás e a
 * faixa continua pendente para a próxima execução.
 */
@Component
@RequiredArgsConstructor
public class SessionReminderSender {

    private static final String PORTAL_HOME = "/colaborador";

    private final SessionReminderRepository reminderRepository;
    private final NotificationService notificationService;

    /**
     * @param body monta o texto a partir da sessão — o que muda entre as faixas
     * @return quantos avisos saíram
     */
    @Transactional
    public int notifyPending(List<CollaboratorSession> candidates, String kind, String title,
            Function<CollaboratorSession, String> body) {
        if (candidates.isEmpty()) {
            return 0;
        }

        // Uma consulta para o lote inteiro: o job roda de minuto em minuto, e
        // perguntar sessão a sessão multiplicaria idas ao banco por nada.
        Set<Long> alreadySent = new HashSet<>(reminderRepository.findAlreadySent(kind,
                candidates.stream().map(session -> session.getId()).toList()));

        int sent = 0;
        for (CollaboratorSession session : candidates) {
            if (alreadySent.contains(session.getId())) {
                continue;
            }

            // Marca primeiro: se duas execuções se cruzarem, a segunda esbarra no
            // índice único em vez de mandar push repetido.
            reminderRepository.save(new SessionReminder(session.getId(), kind));

            notificationService.notifyCollaborator(
                    session.getCollaborator().getId(),
                    NotificationType.SESSION_REMINDER,
                    title,
                    body.apply(session),
                    Map.of("sessionId", session.getId()),
                    PORTAL_HOME);

            sent++;
        }

        return sent;
    }
}
