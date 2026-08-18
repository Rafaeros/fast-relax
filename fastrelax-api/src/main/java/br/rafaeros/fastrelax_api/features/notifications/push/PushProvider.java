package br.rafaeros.fastrelax_api.features.notifications.push;

import br.rafaeros.fastrelax_api.features.notifications.DeviceToken;

/**
 * Um canal de entrega de push.
 *
 * <p>
 * Existe para que {@code NotificationService} não conheça nem FCM nem Web Push:
 * ele pede a entrega e o dispatcher escolhe o provedor pela plataforma do
 * registro. Acrescentar APNs direto, ou trocar o Web Push por outro serviço,
 * não toca em nada fora deste pacote.
 */
public interface PushProvider {

    /** Plataformas que este provedor sabe atender. */
    boolean supports(DeviceToken.Platform platform);

    /** Sem credencial configurada o provedor fica inerte, sem derrubar o envio. */
    boolean isEnabled();

    PushResult send(DeviceToken destination, PushMessage message);
}
