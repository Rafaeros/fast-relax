package br.rafaeros.fastrelax_api.features.notifications;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.rafaeros.fastrelax_api.core.tenancy.CompanyScopedRepository;

public interface DeviceTokenRepository extends CompanyScopedRepository<DeviceToken> {

    /**
     * O token do FCM é único no sistema inteiro: o mesmo aparelho não pode estar
     * registrado em duas empresas. Por isso a busca não filtra por empresa —
     * reinscrever precisa encontrar a linha existente onde quer que ela esteja.
     */
    Optional<DeviceToken> findByToken(String token);

    List<DeviceToken> findByCollaboratorIdAndActiveTrue(Long collaboratorId);

    /**
     * Localiza a inscrição de Web Push pelo endpoint.
     *
     * <p>
     * O endpoint é a identidade da inscrição: o navegador pode rotacionar as
     * chaves mantendo o mesmo endereço, e reinscrever precisa atualizar a linha
     * existente — é isso que o índice único {@code uq_device_tokens_endpoint}
     * garante do lado do banco.
     *
     * <p>
     * Consulta nativa porque JPQL não navega dentro de JSONB.
     */
    @Query(value = "SELECT * FROM device_tokens WHERE push_subscription ->> 'endpoint' = :endpoint",
            nativeQuery = true)
    Optional<DeviceToken> findBySubscriptionEndpoint(@Param("endpoint") String endpoint);
}
