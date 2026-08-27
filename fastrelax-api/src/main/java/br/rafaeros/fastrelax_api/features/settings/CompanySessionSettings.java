package br.rafaeros.fastrelax_api.features.settings;

import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.UpdateTimestamp;

import br.rafaeros.fastrelax_api.core.tenancy.CompanyOwned;
import br.rafaeros.fastrelax_api.features.companies.Company;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Regras de agendamento de uma empresa.
 *
 * <p>
 * Era uma linha única global, garantida por índice em expressão constante.
 * Com vários clientes isso deixou de fazer sentido: a duração da massagem e a
 * antecedência permitida são acordos de cada contrato, não do produto.
 *
 * <p>
 * A chave primária é o próprio {@code company_id}, o que torna a cardinalidade
 * 1:1 impossível de violar — não existe estado em que uma empresa tenha duas
 * configurações.
 */
@Entity
@Table(name = "company_session_settings")
@NoArgsConstructor
@Getter
@Setter
public class CompanySessionSettings implements CompanyOwned {

    /** Usados quando a empresa ainda não tem configuração gravada. */
    public static final int FALLBACK_DURATION_MINUTES = 5;
    public static final int FALLBACK_START_GRACE_MINUTES = 2;
    public static final int FALLBACK_EARLY_START_MINUTES = 2;
    public static final int FALLBACK_MAX_ADVANCE_DAYS = 30;

    @Id
    @Column(name = "company_id")
    private Long companyId;

    @MapsId
    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

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

    /**
     * Minutos de antecedência tolerados para iniciar. Quem chega adiantado não
     * fica esperando o relógio virar em frente à cadeira.
     */
    @ColumnDefault("2")
    @Column(name = "early_start_minutes", nullable = false)
    private int earlyStartMinutes = FALLBACK_EARLY_START_MINUTES;

    /** Quantos dias à frente o colaborador pode agendar, contando a partir de hoje. */
    @ColumnDefault("30")
    @Column(name = "max_advance_days", nullable = false)
    private int maxAdvanceDays = FALLBACK_MAX_ADVANCE_DAYS;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public CompanySessionSettings(Company company) {
        this.company = company;
    }
}
