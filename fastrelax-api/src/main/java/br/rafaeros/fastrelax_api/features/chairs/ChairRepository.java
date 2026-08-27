package br.rafaeros.fastrelax_api.features.chairs;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.rafaeros.fastrelax_api.core.tenancy.CompanyScopedRepository;

public interface ChairRepository extends CompanyScopedRepository<Chair> {

    /**
     * Usada pelo heartbeat, que chega sem tenant no contexto: o ESP32 não faz
     * login e só sabe o próprio MAC. É a cadeira encontrada que revela a empresa.
     */
    Optional<Chair> findByMacAddress(String macAddress);

    List<Chair> findByCompanyIdAndActiveTrue(Long companyId);

    /**
     * Todas as cadeiras ativas, de todas as empresas.
     *
     * <p>
     * É para o monitor de presença, que roda sem requisição e cujo interesse é
     * justamente o parque inteiro — a Physical precisa saber de um equipamento
     * mudo em qualquer cliente.
     */
    List<Chair> findByActiveTrue();

    /**
     * Query nativa para enxergar cadeiras removidas: o MAC continua ocupando a
     * constraint de unicidade, então reinstalar um dispositivo antigo precisa
     * reativar a linha em vez de inserir outra.
     *
     * <p>
     * Sem {@code company_id} de propósito — a unicidade do MAC é global, e a
     * checagem precisa alcançar o equipamento mesmo que ele esteja cadastrado em
     * outro cliente. Quem chama trata esse caso como conflito, sem revelar de
     * quem é a cadeira.
     */
    @Query(value = "SELECT * FROM chairs WHERE UPPER(mac_address) = UPPER(:macAddress)", nativeQuery = true)
    Optional<Chair> findByMacAddressIncludingDeleted(@Param("macAddress") String macAddress);
}
