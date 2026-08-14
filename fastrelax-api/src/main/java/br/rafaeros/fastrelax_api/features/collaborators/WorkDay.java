package br.rafaeros.fastrelax_api.features.collaborators;

/**
 * Business days a collaborator can have an allowed session window on.
 *
 * <p>
 * Deliberately not {@link java.time.DayOfWeek}: that enum carries Sunday, which
 * the {@code day_of_week} CHECK constraint rejects. Keeping a separate type
 * makes the restriction explicit and avoids ambiguous imports wherever both
 * types are in scope.
 */
public enum WorkDay {
    MONDAY("Segunda-feira"),
    TUESDAY("Terça-feira"),
    WEDNESDAY("Quarta-feira"),
    THURSDAY("Quinta-feira"),
    FRIDAY("Sexta-feira"),
    SATURDAY("Sábado");

    private final String label;

    WorkDay(String label) {
        this.label = label;
    }

    /** Texto para exibição; o nome do enum segue sendo o valor trafegado. */
    public String getLabel() {
        return label;
    }

    /** Vazio para domingo, único dia sem expediente. */
    public static java.util.Optional<WorkDay> from(java.time.LocalDate date) {
        return java.util.Arrays.stream(values())
                .filter(day -> day.name().equals(date.getDayOfWeek().name()))
                .findFirst();
    }
}
