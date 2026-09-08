package br.rafaeros.fastrelax_api.features.collaborators;

/**
 * Lifecycle of a rest session.
 *
 * <pre>
 * SCHEDULED ──start──▶ STARTED ──finish──▶ DONE
 *     │                     │
 *     │                     └──(chegou ao endTime)──▶ DONE
 *     │
 *     ├──cancel──▶ CANCELLED ◀──cancel──┘
 *     │
 *     └──(não iniciou até a tolerância)──▶ EXPIRED
 * </pre>
 *
 * {@link #SCHEDULED} and {@link #STARTED} are the "active" states: they are the
 * ones that occupy the collaborator's quota, whose limit and period each company
 * configures in {@code company_session_settings}.
 *
 * <p>
 * {@link #EXPIRED} significa uma coisa só: não compareceu. Sessão iniciada que
 * roda até o fim do horário vira {@link #DONE} mesmo sem ninguém apertar
 * "finalizar" — a duração foi cumprida e o relé desligou sozinho. O
 * {@code finish} manual serve para encerrar antes do previsto.
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
