package br.rafaeros.fastrelax_api.features.notifications.push;

/**
 * Desfecho de uma tentativa de entrega.
 *
 * <p>
 * A distinção que importa é entre {@link #GONE} e {@link #FAILED}: o primeiro
 * significa que aquele destino não existe mais (o usuário desinstalou o app ou
 * revogou a permissão) e a linha deve ser desativada; o segundo é falha
 * passageira — rede, serviço de push fora do ar — e o destino continua válido.
 */
public enum PushResult {

    /** Aceito pelo serviço de push. */
    DELIVERED,

    /** Destino morto: desativar o registro para não tentar de novo. */
    GONE,

    /** Falha temporária; o destino segue ativo. */
    FAILED,

    /** Provedor sem credencial configurada — nada foi tentado. */
    SKIPPED
}
