package br.rafaeros.fastrelax_api.features.users;

/**
 * Papéis do painel, em dois planos que não se misturam.
 *
 * <p>
 * {@link #SYSADMIN} é a equipe da Physical: administra empresas, firmwares e os
 * gestores de cada cliente. Não pertence a empresa nenhuma e, de propósito, não
 * alcança dado operacional — colaborador, sessão e notificação carregam CPF e
 * rotina de pessoas que não são clientes dela.
 *
 * <p>
 * {@link #COMPANY_ADMIN} e {@link #COMPANY_RH} vivem dentro de uma empresa e só
 * enxergam o que é dela. A diferença entre os dois é de alcance administrativo:
 * o gestor cria usuários do painel, o RH opera o dia a dia.
 */
public enum UserRole {

    SYSADMIN("Administrador da plataforma"),
    COMPANY_ADMIN("Gestor da empresa"),
    COMPANY_RH("RH da empresa");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    /** Texto para exibição; o nome do enum segue sendo o valor trafegado. */
    public String getLabel() {
        return label;
    }

    /** Equipe da plataforma: opera entre empresas, não dentro de uma. */
    public boolean isPlatform() {
        return this == SYSADMIN;
    }

    /** Papéis que exigem — e só operam sob — um vínculo com empresa. */
    public boolean isCompanyScoped() {
        return !isPlatform();
    }
}
