package br.rafaeros.fastrelax_api.features.notifications.dtos;

import br.rafaeros.fastrelax_api.features.notifications.DeviceToken;
import br.rafaeros.fastrelax_api.features.notifications.PushSubscription;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/**
 * Registro de um destino de push.
 *
 * <p>
 * Um corpo para as duas tecnologias: o app Android manda {@code token} (do FCM)
 * e o navegador manda {@code pushSubscription} (do Web Push). Qual dos dois vem
 * preenchido depende da plataforma, e a checagem abaixo é a mesma que a
 * constraint do banco aplica.
 *
 * @param token            token do FCM — obrigatório em ANDROID e IOS
 * @param pushSubscription inscrição do navegador — obrigatória em WEB
 */
public record RegisterDeviceTokenDTO(
    String token,

    @Valid
    PushSubscription pushSubscription,

    @NotNull(message = "A plataforma é obrigatória")
    DeviceToken.Platform platform
) {

    @AssertTrue(message = "Informe o token (Android/iOS) ou a inscrição de push (Web) conforme a plataforma")
    public boolean isDestinationConsistent() {
        if (platform == null) {
            // Sem plataforma o @NotNull acima já reprova; não vale acusar duas vezes.
            return true;
        }
        return platform == DeviceToken.Platform.WEB
                ? pushSubscription != null
                : token != null && !token.isBlank();
    }
}
