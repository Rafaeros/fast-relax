package br.rafaeros.fastrelax_api.features.settings;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * Em que janela a cota de massagens de um colaborador é contada.
 *
 * <p>
 * {@link #ACTIVE} é diferente das outras três: não olha calendário nenhum,
 * conta o que está marcado ou em andamento <em>agora</em>. É a regra que o
 * sistema tinha fixa — "uma massagem por vez" — e continua sendo o padrão.
 *
 * <p>
 * As demais contam pela data da sessão, não pela data em que ela foi marcada:
 * o acordo é "uma massagem por semana", e quem marca na sexta para a semana que
 * vem não gastou a desta.
 */
public enum SessionQuotaPeriod {

    ACTIVE("massagens marcadas ao mesmo tempo"),
    DAY("por dia"),
    WEEK("por semana"),
    MONTH("por mês");

    private final String label;

    SessionQuotaPeriod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Falso só para {@link #ACTIVE}, o único que não tem começo e fim no calendário. */
    public boolean isWindowed() {
        return this != ACTIVE;
    }

    /**
     * Primeiro dia da janela que contém {@code date}.
     *
     * <p>
     * A semana começa na segunda (ISO-8601), que é como a escala de trabalho é
     * lida aqui — o sábado do quadro de horários fecha a semana, não abre a
     * seguinte.
     */
    public LocalDate startOf(LocalDate date) {
        return switch (this) {
            case DAY -> date;
            case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> date.withDayOfMonth(1);
            case ACTIVE -> date;
        };
    }

    /** Último dia da janela que contém {@code date}, inclusivo. */
    public LocalDate endOf(LocalDate date) {
        return switch (this) {
            case DAY -> date;
            case WEEK -> date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            case MONTH -> date.with(TemporalAdjusters.lastDayOfMonth());
            case ACTIVE -> date;
        };
    }
}
