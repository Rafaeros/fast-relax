package br.rafaeros.fastrelax_api.features.notifications;

import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import br.rafaeros.fastrelax_api.features.collaborators.SessionLifecycleEvent;
import lombok.RequiredArgsConstructor;

/**
 * Traduz o que aconteceu com a sessão em um aviso para o colaborador.
 *
 * <p>
 * É o único lugar que sabe o texto de cada evento. As regras de sessão publicam
 * o fato e ignoram o resto — mudar a redação, acrescentar um canal ou silenciar
 * um tipo de aviso acontece aqui, sem tocar no agendamento.
 */
@Component
@RequiredArgsConstructor
public class SessionNotificationListener {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM");

    private static final String PORTAL_HOME = "/colaborador";

    private final NotificationService notificationService;

    /**
     * Só depois do commit: avisar durante a transação criaria a chance de o
     * colaborador receber "massagem agendada" para uma sessão que acabou
     * revertida por erro de gravação — e push, uma vez entregue, não volta.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onSessionEvent(SessionLifecycleEvent event) {
        String horario = event.startTime().format(TIME);
        String dia = event.sessionDate().format(DATE);

        NotificationType type;
        String title;
        String body;

        switch (event.type()) {
            case SCHEDULED -> {
                type = NotificationType.SESSION_SCHEDULED;
                title = "Massagem agendada";
                body = "Sua massagem está marcada para " + dia + " às " + horario + ".";
            }
            case STARTED -> {
                type = NotificationType.SESSION_STARTED;
                title = "Massagem iniciada";
                body = "Bom descanso! A cadeira desliga sozinha às "
                        + event.endTime().format(TIME) + ".";
            }
            case FINISHED -> {
                type = NotificationType.SESSION_FINISHED;
                title = "Massagem concluída";
                body = "Sua massagem das " + horario + " terminou. Até a próxima!";
            }
            case EXPIRED -> {
                type = NotificationType.SESSION_EXPIRED;
                title = "Massagem expirada";
                body = "Sua massagem das " + horario + " não foi iniciada e o horário foi liberado.";
            }
            case CANCELLED -> {
                type = NotificationType.SESSION_CANCELLED;
                title = "Massagem cancelada";
                body = "Sua massagem de " + dia + " às " + horario + " foi cancelada.";
            }
            default -> {
                return;
            }
        }

        notificationService.notifyCollaborator(
                event.collaboratorId(),
                type,
                title,
                body,
                Map.of("sessionId", event.sessionId()),
                PORTAL_HOME);
    }
}
