package br.rafaeros.fastrelax_api.features.users;

import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;

import br.rafaeros.fastrelax_api.core.tenancy.TenantContext;

/**
 * Filtros combináveis de {@link User}.
 *
 * <p>
 * O isolamento não vem do {@code CompanyScopedRepository} porque o vínculo com
 * empresa é opcional nesta entidade — o SYSADMIN não pertence a nenhuma. O
 * predicado abaixo faz o mesmo papel, com essa exceção embutida.
 */
public final class UserSpecifications {

    private UserSpecifications() {
    }

    /**
     * Quem opera dentro de uma empresa só enxerga os usuários dela. Escopo de
     * plataforma enxerga todos — é o SYSADMIN administrando os gestores dos
     * clientes.
     */
    public static Specification<User> visibleToCurrentTenant() {
        return (root, query, cb) -> {
            Optional<Long> companyId = TenantContext.currentCompanyId();
            if (companyId.isEmpty()) {
                return null;
            }
            return cb.equal(root.get("company").get("id"), companyId.get());
        };
    }

    public static Specification<User> hasRole(UserRole role) {
        return (root, query, cb) -> role == null ? null : cb.equal(root.get("role"), role);
    }

    public static Specification<User> nameContains(String name) {
        return (root, query, cb) -> (name == null || name.isBlank())
                ? null
                : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<User> hasActive(Boolean active) {
        return (root, query, cb) -> active == null ? null : cb.equal(root.get("active"), active);
    }
}
