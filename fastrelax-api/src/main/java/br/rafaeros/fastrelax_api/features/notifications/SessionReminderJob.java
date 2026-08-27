package br.rafaeros.fastrelax_api.features.notifications;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import br.rafaeros.fastrelax_api.features.collaborators.CollaboratorSession;
import br.rafaeros.fastrelax_api.features.collaborators.CollaboratorSessionRepository;
import br.rafaeros.fastrelax_api.features.collaborators.SessionExpirationSweeper;
import br.rafaeros.fastrelax_api.features.collaborators.SessionStatus;
import lombok.RequiredArgsConstructor;

/**
 * Lembretes de massagem, em duas cadências diferentes de propósito.
 *
 * <p>
 * <b>Rolantes</b> ({@code app.notifications.reminder-offsets}, por padrão 60 e 5
 * minutos) usam janela com teto: a faixa dispara quando o início da sessão entra
 * em {@code agora + offset}. Cada faixa acontece no seu próprio momento, e
 * acrescentar uma é acrescentar um número na configuração.
 *
 * <p>
 * <b>Véspera</b> é por calendário, não por offset. Tratar "1 dia antes" como
 * 1440 minutos rolantes quebraria: quem agenda hoje para amanhã — menos de 24h
 * de antecedência — casaria com a janela no instante do agendamento e receberia
 * "sua massagem é amanhã" junto da confirmação. O resumo diário evita isso
 * olhando a agenda do dia seguinte num horário fixo.
 */
@Component
@RequiredArgsConstructor
public class SessionReminderJob {

    private static final Logger log = LoggerFactory.getLogger(SessionReminderJob.class);

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final CollaboratorSessionRepository sessionRepository;
    private final SessionExpirationSweeper sessionExpirationSweeper;
    private final SessionReminderSender sender;

    /** Antecedências das faixas rolantes, em minutos. */
    @Value("${app.notifications.reminder-offsets:60,5}")
    private List<Integer> reminderOffsets;

    @Value("${app.notifications.daily-digest-cron:0 0 10 * * *}")
    private String dailyDigestCron;

    /**
     * Tick curto porque a menor faixa manda: com intervalo de 10 minutos, o
     * lembrete de 5 minutos poderia sair depois do horário da massagem —
     * atrasado, seria pior que nenhum.
     */
    @Scheduled(fixedDelayString = "${app.notifications.reminder-interval-ms:60000}")
    public void sendRollingReminders() {
        LocalDateTime now = LocalDateTime.now();

        for (int minutesBefore : reminderOffsets) {
            List<CollaboratorSession> candidates = sessionRepository
                    .findScheduledStartingBetween(now, now.plusMinutes(minutesBefore));

            sender.notifyPending(candidates, SessionReminder.rollingKind(minutesBefore),
                    "Sua massagem está próxima",
                    session -> "Sua massagem é " + describe(minutesBefore) + ", às "
                            + session.getStartTime().format(TIME) + ".");
        }
    }

    @Scheduled(cron = "${app.notifications.daily-digest-cron:0 0 10 * * *}")
    public void sendDailyDigest() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        List<CollaboratorSession> agenda = sessionRepository
                .findBySessionDateAndStatus(tomorrow, SessionStatus.SCHEDULED);

        int sent = sender.notifyPending(agenda, SessionReminder.DAY_BEFORE, "Massagem amanhã",
                session -> "Sua massagem é amanhã às " + session.getStartTime().format(TIME) + ".");

        if (sent > 0) {
            log.info("Resumo da véspera: {} colaborador(es) avisado(s) sobre {}", sent, tomorrow);
        }
    }

    /**
     * Varredura de inicialização.
     *
     * <p>
     * A máquina é ligada todo dia, então subir é o momento em que mais coisa está
     * atrasada: sessões de ontem que ninguém iniciou continuam marcadas como
     * agendadas, e as faixas que venceram com o sistema desligado nunca rodaram.
     *
     * <p>
     * O resumo da véspera só entra se o horário do cron já passou hoje — subir às
     * 8h não deve antecipar o envio das 10h.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void catchUpOnStartup() {
        int closed = sessionExpirationSweeper.sweepAllCompanies();
        if (closed > 0) {
            log.info("Inicialização: {} sessão(ões) encerrada(s) por atraso", closed);
        }

        sendRollingReminders();

        if (digestAlreadyDueToday()) {
            log.info("Inicialização: horário do resumo diário já passou hoje, enviando o que faltou");
            sendDailyDigest();
        }
    }

    /**
     * O horário do resumo já passou hoje?
     *
     * <p>
     * Perguntado ao próprio cron em vez de a uma hora fixa no código: mudar a
     * expressão na configuração continua funcionando sem ninguém lembrar de
     * ajustar esta checagem.
     */
    private boolean digestAlreadyDueToday() {
        try {
            LocalDateTime firstRunToday = CronExpression.parse(dailyDigestCron)
                    .next(LocalDate.now().atStartOfDay());

            return firstRunToday != null && !firstRunToday.isAfter(LocalDateTime.now());
        } catch (IllegalArgumentException e) {
            log.warn("Cron do resumo diário inválido ({}): {}", dailyDigestCron, e.getMessage());
            return false;
        }
    }

    /** "em 1 hora", "em 2 horas", "em 5 minutos" — derivado do offset da faixa. */
    private String describe(int minutes) {
        if (minutes >= 60 && minutes % 60 == 0) {
            int hours = minutes / 60;
            return hours == 1 ? "em 1 hora" : "em " + hours + " horas";
        }
        return "em " + minutes + " minutos";
    }
}
