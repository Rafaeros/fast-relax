package br.rafaeros.fastrelax_api.features.notifications;

/**
 * Motivos pelos quais o colaborador recebe um aviso.
 *
 * <p>
 * O nome do enum é o contrato com o cliente (estável, serve para filtrar e
 * escolher ícone); o label é o que aparece na tela.
 */
public enum NotificationType {

    SESSION_SCHEDULED("Massagem agendada"),
    SESSION_REMINDER("Lembrete de massagem"),
    SESSION_STARTED("Massagem iniciada"),
    SESSION_FINISHED("Massagem concluída"),
    SESSION_EXPIRED("Massagem expirada"),
    SESSION_CANCELLED("Massagem cancelada");

    private final String label;

    NotificationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
