package br.rafaeros.fastrelax_api.features.chairs.dtos;

/**
 * Desfecho do envio da configuração de rede para uma cadeira.
 *
 * <p>
 * Uma linha por equipamento, porque o envio em lote continua mesmo quando uma
 * cadeira não responde: a lista mostra quais foram e quais faltaram, em vez de
 * um "falhou" que não diz onde ir olhar.
 *
 * @param outcome código do {@code ChairCommandResult.Outcome}, para a tela
 *                distinguir os casos sem interpretar texto
 * @param message pronta para quem está na planta com o notebook na mão
 */
public record ChairNetworkResultDTO(
    Long chairId,
    String chairName,
    boolean delivered,
    String outcome,
    String message
) {}
