package br.rafaeros.fastrelax_api.features.firmwares;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Catálogo da plataforma, não de um cliente: nenhuma consulta aqui é escopada
 * por empresa, porque a mesma versão roda em cadeiras de clientes diferentes.
 */
public interface FirmwareRepository extends JpaRepository<Firmware, Long>, JpaSpecificationExecutor<Firmware> {

    Optional<Firmware> findByVersion(String version);

    /**
     * Enxerga também as versões removidas: a constraint {@code UNIQUE} da coluna
     * {@code version} não conhece soft delete, então republicar uma versão exige
     * reativar a linha em vez de inserir outra.
     */
    @Query(value = "SELECT * FROM firmwares WHERE version = :version", nativeQuery = true)
    Optional<Firmware> findByVersionIncludingDeleted(@Param("version") String version);
}
