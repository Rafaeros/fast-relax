package br.rafaeros.fastrelax_api.features.departments;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.rafaeros.fastrelax_api.core.tenancy.CompanyScopedRepository;

public interface DepartmentRepository extends CompanyScopedRepository<Department> {

    /**
     * Busca dentro da empresa, sem diferenciar maiúsculas nem espaços nas pontas.
     *
     * <p>
     * A constraint {@code uq_departments_company_name} é sensível a caixa, então
     * "Recursos Humanos" e "RECURSOS HUMANOS" passariam como dois departamentos
     * distintos. Comparar em minúsculas dos dois lados faz a importação
     * reaproveitar o que já existe em vez de duplicar com outra grafia.
     *
     * <p>
     * Query nativa para escapar do {@code @SQLRestriction("deleted_at IS NULL")} e
     * enxergar também os removidos, que continuam ocupando o nome na constraint.
     * Como ela não passa pelo {@code Specification}, o {@code company_id} vai
     * explícito — é a única forma de o filtro de tenant alcançar esta consulta.
     */
    @Query(value = """
            SELECT * FROM departments
            WHERE company_id = :companyId
              AND LOWER(TRIM(name)) = LOWER(TRIM(:name))
            """, nativeQuery = true)
    Optional<Department> findByNameIncludingDeleted(@Param("companyId") Long companyId,
            @Param("name") String name);
}
