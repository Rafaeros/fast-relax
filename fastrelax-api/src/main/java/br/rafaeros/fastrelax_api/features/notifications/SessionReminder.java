package br.rafaeros.fastrelax_api.features.notifications;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Marca de que uma faixa de lembrete já foi enviada para uma sessão.
 *
 * <p>
 * Existe porque as faixas são independentes: mandar o de 1 hora não pode
 * silenciar o de 5 minutos. Uma linha por (sessão, faixa), com índice único, é
 * o que garante isso mesmo se duas execuções se cruzarem.
 *
 * <p>
 * Guarda o id da sessão como valor, não como relação: o job só precisa saber
 * "já mandei?", e uma associação obrigaria a carregar a sessão inteira para
 * responder.
 */
@Entity
@Table(name = "session_reminders")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SessionReminder {

    /** Resumo diário: enviado na véspera, pelo relógio, não por offset. */
    public static final String DAY_BEFORE = "DAY_BEFORE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(nullable = false, length = 20)
    private String kind;

    @CreationTimestamp
    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;

    public SessionReminder(Long sessionId, String kind) {
        this.sessionId = sessionId;
        this.kind = kind;
    }

    /** Faixa rolante identificada pelo próprio offset: 60 min vira {@code T-60}. */
    public static String rollingKind(int minutesBefore) {
        return "T-" + minutesBefore;
    }
}
