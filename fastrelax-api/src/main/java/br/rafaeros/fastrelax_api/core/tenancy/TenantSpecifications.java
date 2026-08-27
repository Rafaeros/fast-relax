package br.rafaeros.fastrelax_api.core.tenancy;

import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

/**
 * O predicado de isolamento, em um lugar só.
 *
 * <p>
 * Devolver {@code null} quando não há empresa no contexto é o mesmo contrato das
 * demais {@code Specification}s do projeto: o filtro simplesmente some da
 * composição. É o que deixa a equipe Physical e as rotinas de fundo lerem entre
 * empresas sem uma segunda versão de cada consulta.
 *
 * <p>
 * Como o escopo de plataforma é aberto, a barreira de quem pode usá-lo fica nos
 * {@code @PreAuthorize} dos controllers — SYSADMIN não alcança colaborador nem
 * sessão por não ter rota, não por não ter predicado.
 */
public final class TenantSpecifications {

    /** Caminho padrão até a empresa nas entidades que herdam de {@link CompanyScopedEntity}. */
    private static final String[] DIRECT = { "company" };

    private TenantSpecifications() {
    }

    public static <T> Specification<T> currentCompany() {
        return currentCompany(DIRECT);
    }

    /**
     * Para o que se liga à empresa por tabela vizinha, como o horário permitido,
     * que chega lá via colaborador.
     *
     * @param pathToCompany atributos a percorrer até a associação {@code Company}
     */
    public static <T> Specification<T> currentCompany(String... pathToCompany) {
        return (root, query, cb) -> {
            Optional<Long> companyId = TenantContext.currentCompanyId();
            if (companyId.isEmpty()) {
                return null;
            }
            return cb.equal(navigate(root, pathToCompany).get("id"), companyId.get());
        };
    }

    public static <T> Specification<T> hasId(Long id) {
        return (root, query, cb) -> id == null ? null : cb.equal(root.get("id"), id);
    }

    private static Path<?> navigate(Root<?> root, String... path) {
        Path<?> current = root;
        for (String attribute : path) {
            current = current.get(attribute);
        }
        return current;
    }
}
