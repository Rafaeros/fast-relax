package br.rafaeros.fastrelax_api.features.notifications;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Inscrição de Web Push, exatamente como o navegador a entrega.
 *
 * <p>
 * O {@code endpoint} é a URL do serviço de push do próprio navegador (FCM no
 * Chrome, Mozilla autopush no Firefox, WNS no Edge) — é para lá que a API envia.
 * As duas chaves não são identificação: elas cifram o conteúdo de ponta a ponta,
 * então nem o serviço de push consegue ler a mensagem que passa por ele.
 *
 * <p>
 * Serializada como JSONB na coluna {@code device_tokens.push_subscription}, e
 * também é o corpo que o navegador manda no registro — o mesmo formato dos dois
 * lados evita conversão no meio.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PushSubscription(
        @NotBlank(message = "O endpoint da inscrição é obrigatório")
        String endpoint,

        @NotNull(message = "As chaves da inscrição são obrigatórias")
        @Valid
        Keys keys) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Keys(
            @NotBlank(message = "A chave p256dh é obrigatória")
            String p256dh,

            @NotBlank(message = "A chave auth é obrigatória")
            String auth) {
    }
}
