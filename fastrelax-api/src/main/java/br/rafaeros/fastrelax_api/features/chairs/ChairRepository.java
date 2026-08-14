package br.rafaeros.fastrelax_api.features.chairs;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChairRepository extends JpaRepository<Chair, Long>, JpaSpecificationExecutor<Chair> {

    Optional<Chair> findByMacAddress(String macAddress);

    List<Chair> findByActiveTrue();

    /**
     * Query nativa para enxergar cadeiras removidas: o MAC continua ocupando a
     * constraint de unicidade, então reinstalar um dispositivo antigo precisa
     * reativar a linha em vez de inserir outra.
     */
    @Query(value = "SELECT * FROM chairs WHERE UPPER(mac_address) = UPPER(:macAddress)", nativeQuery = true)
    Optional<Chair> findByMacAddressIncludingDeleted(@Param("macAddress") String macAddress);
}
