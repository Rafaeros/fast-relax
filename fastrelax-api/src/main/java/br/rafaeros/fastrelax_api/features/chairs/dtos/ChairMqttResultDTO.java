package br.rafaeros.fastrelax_api.features.chairs.dtos;

/**
 * Desfecho do envio da configuração de MQTT para uma cadeira.
 *
 * <p>
 * Espelha {@link ChairNetworkResultDTO}: mesma forma, mesmo motivo — quem
 * chama recebe um desfecho por equipamento em vez de um booleano que juntaria
 * "recusou com motivo" e "não respondeu nada".
 *
 * @param outcome código do {@code ChairCommandResult.Outcome}, para a tela
 *                distinguir os casos sem interpretar texto
 * @param message pronta para quem está na planta com o notebook na mão
 */
public record ChairMqttResultDTO(
    Long chairId,
    String chairName,
    boolean delivered,
    String outcome,
    String message
) {}
