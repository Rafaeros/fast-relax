package br.rafaeros.fastrelax_api.features.users;

public enum UserRole {
    ADMIN("Administrador"),
    RH("RH");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    /** Texto para exibição; o nome do enum segue sendo o valor trafegado. */
    public String getLabel() {
        return label;
    }
}