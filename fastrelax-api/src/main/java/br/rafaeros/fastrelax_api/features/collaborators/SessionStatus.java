package br.rafaeros.fastrelax_api.features.collaborators;

/**
 * Lifecycle of a rest session.
 *
 * <pre>
 * SCHEDULED ──start──▶ STARTED ──finish──▶ DONE
 *     │                     │
 *     ├──cancel──▶ CANCELLED ◀──cancel──┘
 *     │                     │
 *     │                     └──(não finalizou)──▶ EXPIRED
 *     │
 *     └──(não iniciou até a tolerância)──▶ EXPIRED
 * </pre>
 *
 * {@link #SCHEDULED} and {@link #STARTED} are the "active" states covered by
 * the partial unique index {@code uq_collaborator_active_session}, which allows
 * only one of them per collaborator at a time.
 *
 * <p>
 * {@link #EXPIRED} cobre os dois casos de abandono: não compareceu e não
 * finalizou. Em ambos o horário é devolvido para outra pessoa.
 */
public enum SessionStatus {
    SCHEDULED,
    STARTED,
    DONE,
    EXPIRED,
    CANCELLED;

    public boolean isActive() {
        return this == SCHEDULED || this == STARTED;
    }
}
