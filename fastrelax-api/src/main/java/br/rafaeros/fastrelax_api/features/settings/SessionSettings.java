package br.rafaeros.fastrelax_api.features.settings;

import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
 * Configuração global das sessões. Sempre existe uma única linha — o índice
 * {@code uq_session_settings_singleton} garante isso no banco.
 */
@Entity
@Table(name = "session_settings")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SessionSettings {

    /** Usados quando a tabela ainda não foi populada. */
    public static final int FALLBACK_DURATION_MINUTES = 5;
    public static final int FALLBACK_START_GRACE_MINUTES = 2;
    public static final int FALLBACK_MAX_ADVANCE_DAYS = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ColumnDefault("5")
    @Column(name = "default_duration_minutes", nullable = false)
    private int defaultDurationMinutes = FALLBACK_DURATION_MINUTES;

    /**
     * Minutos de tolerância para iniciar a sessão. Passou disso sem iniciar, ela
     * vira EXPIRED e o horário volta a ficar disponível.
     */
    @ColumnDefault("2")
    @Column(name = "start_grace_minutes", nullable = false)
    private int startGraceMinutes = FALLBACK_START_GRACE_MINUTES;

    /** Quantos dias à frente o colaborador pode agendar, contando a partir de hoje. */
    @ColumnDefault("30")
    @Column(name = "max_advance_days", nullable = false)
    private int maxAdvanceDays = FALLBACK_MAX_ADVANCE_DAYS;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
