package br.rafaeros.fastrelax_api.features.companies;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyRepository extends JpaRepository<Company, Long>, JpaSpecificationExecutor<Company> {

    /** O CNPJ é gravado só com dígitos, então quem chama normaliza antes. */
    Optional<Company> findByCnpj(String cnpj);

    boolean existsByCnpj(String cnpj);

    boolean existsByEmail(String email);

    /** O slug é gravado já em minúsculas, então quem chama normaliza antes. */
    Optional<Company> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /**
     * Enxerga também as empresas removidas.
     *
     * <p>
     * A entidade carrega {@code @SQLRestriction("deleted_at IS NULL")}, mas as
     * constraints {@code UNIQUE} de CNPJ, e-mail e slug não conhecem soft
     * delete. Sem esta consulta, recadastrar uma empresa removida passaria pela
     * checagem de negócio e estouraria como violação de integridade no meio do
     * insert.
     */
    @Query(value = "SELECT * FROM companies WHERE cnpj = :cnpj", nativeQuery = true)
    Optional<Company> findByCnpjIncludingDeleted(@Param("cnpj") String cnpj);

    @Query(value = "SELECT COUNT(*) > 0 FROM companies WHERE email = :email", nativeQuery = true)
    boolean existsByEmailIncludingDeleted(@Param("email") String email);

    @Query(value = "SELECT COUNT(*) > 0 FROM companies WHERE slug = :slug", nativeQuery = true)
    boolean existsBySlugIncludingDeleted(@Param("slug") String slug);
}
