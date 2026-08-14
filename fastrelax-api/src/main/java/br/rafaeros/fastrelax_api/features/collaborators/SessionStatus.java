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
    SCHEDULED("Agendada"),
    STARTED("Em andamento"),
    DONE("Concluída"),
    EXPIRED("Expirada"),
    CANCELLED("Cancelada");

    private final String label;

    SessionStatus(String label) {
        this.label = label;
    }

    /**
     * Texto para exibição.
     *
     * <p>
     * O nome do enum continua sendo o valor trafegado — ele é o que a CHECK
     * constraint do banco aceita e o que os clientes comparam. A tradução vai
     * num campo separado do DTO, para a tela não precisar de um dicionário
     * próprio e mudanças de redação não quebrarem integração.
     */
    public String getLabel() {
        return label;
    }

    public boolean isActive() {
        return this == SCHEDULED || this == STARTED;
    }
}
