package br.rafaeros.fastrelax_api.features.departments;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepartmentRepository
        extends JpaRepository<Department, Long>, JpaSpecificationExecutor<Department> {

    /**
     * Busca sem diferenciar maiúsculas nem espaços nas pontas.
     *
     * <p>
     * O {@code UNIQUE(name)} do Postgres é sensível a caixa, então "Recursos
     * Humanos" e "RECURSOS HUMANOS" passariam como dois departamentos distintos.
     * Comparar em minúsculas dos dois lados faz a importação reaproveitar o que já
     * existe em vez de duplicar com outra grafia.
     *
     * <p>
     * Query nativa para escapar do {@code @SQLRestriction("deleted_at IS NULL")} e
     * enxergar também os removidos, que continuam ocupando o nome na constraint.
     */
    @Query(value = "SELECT * FROM departments WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name))",
            nativeQuery = true)
    Optional<Department> findByNameIncludingDeleted(@Param("name") String name);
}
