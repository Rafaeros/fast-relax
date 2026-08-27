package br.rafaeros.fastrelax_api.core.tenancy;

/**
 * Quem autentica sabe dizer de qual empresa é.
 *
 * <p>
 * Implementado por {@code User} e {@code Collaborator}, é o que permite ao
 * {@link TenantContextFilter} resolver o tenant sem conhecer os dois tipos —
 * um terceiro tipo de credencial entra no isolamento só por implementar isto.
 */
public interface TenantPrincipal {

    /**
     * @return a empresa do autenticado, ou {@code null} para a equipe da
     *         plataforma, que não pertence a nenhuma
     */
    Long tenantCompanyId();
}
