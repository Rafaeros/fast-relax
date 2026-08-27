package br.rafaeros.fastrelax_api.core.tenancy;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Repositório de entidade que pertence a uma empresa.
 *
 * <p>
 * Os métodos {@code ...Scoped} são os que devem ser usados no fluxo normal: eles
 * grudam o predicado de tenant em qualquer {@code Specification} recebida, então
 * não existe consulta "quase certa" que alguém esqueceu de filtrar.
 *
 * <p>
 * {@link #findByIdScoped(Long)} existe por um motivo específico: o
 * {@code findById} herdado busca pela chave primária e não passa por
 * {@code Specification} nenhuma — com um id adivinhado, ele devolveria o
 * registro de outra empresa. Toda leitura por id no fluxo de requisição usa a
 * versão escopada; o {@code findById} cru fica para as rotinas de plataforma,
 * que declaram esse escopo de propósito.
 */
@NoRepositoryBean
public interface CompanyScopedRepository<T extends CompanyOwned>
        extends JpaRepository<T, Long>, JpaSpecificationExecutor<T> {

    default Optional<T> findByIdScoped(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return findOne(scoped(TenantSpecifications.hasId(id)));
    }

    default Page<T> findAllScoped(Specification<T> spec, Pageable pageable) {
        return findAll(scoped(spec), pageable);
    }

    default List<T> findAllScoped(Specification<T> spec) {
        return findAll(scoped(spec));
    }

    default long countScoped(Specification<T> spec) {
        return count(scoped(spec));
    }

    default boolean existsScoped(Specification<T> spec) {
        return exists(scoped(spec));
    }

    private Specification<T> scoped(Specification<T> spec) {
        return Specification.allOf(spec, TenantSpecifications.currentCompany());
    }
}
