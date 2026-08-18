package br.rafaeros.fastrelax_api.features.notifications.push;

import java.security.Security;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.rafaeros.fastrelax_api.features.notifications.DeviceToken;
import br.rafaeros.fastrelax_api.features.notifications.PushSubscription;
import jakarta.annotation.PostConstruct;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;

/**
 * Entrega para navegador pelo protocolo Web Push.
 *
 * <p>
 * Não passa por Firebase: o navegador informa na inscrição o endereço do próprio
 * serviço de push (Google no Chrome, Mozilla no Firefox, Microsoft no Edge) e a
 * API envia direto para lá. O par de chaves VAPID é a identidade do servidor
 * nesse protocolo — a pública vai para o navegador no momento da inscrição, a
 * privada assina cada envio e nunca sai daqui.
 *
 * <p>
 * O corpo vai cifrado com as chaves da própria inscrição, então o serviço de
 * push repassa sem conseguir ler o conteúdo.
 */
@Component
public class WebPushProvider implements PushProvider {

    private static final Logger log = LoggerFactory.getLogger(WebPushProvider.class);

    @Value("${app.push.webpush.public-key:}")
    private String publicKey;

    @Value("${app.push.webpush.private-key:}")
    private String privateKey;

    /** Contato do responsável, exigido pelo VAPID: {@code mailto:} ou URL. */
    @Value("${app.push.webpush.subject:mailto:ti@fastrelax.local}")
    private String subject;

    private final ObjectMapper objectMapper;

    private PushService pushService;

    public WebPushProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void init() {
        if (publicKey.isBlank() || privateKey.isBlank()) {
            log.info("Web Push desligado: gere um par VAPID e defina "
                    + "app.push.webpush.public-key/private-key para habilitar");
            return;
        }

        try {
            // A JVM não traz as curvas elípticas usadas pelo VAPID; o
            // BouncyCastle precisa estar registrado antes de montar o serviço.
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }

            pushService = new PushService(publicKey, privateKey, subject);
            log.info("Web Push habilitado");
        } catch (Exception e) {
            log.error("Falha ao inicializar o Web Push: {}", e.getMessage());
        }
    }

    @Override
    public boolean supports(DeviceToken.Platform platform) {
        return platform == DeviceToken.Platform.WEB;
    }

    @Override
    public boolean isEnabled() {
        return pushService != null;
    }

    /** Chave pública em base64url — o navegador precisa dela para se inscrever. */
    public String getPublicKey() {
        return publicKey;
    }

    @Override
    public PushResult send(DeviceToken destination, PushMessage message) {
        if (!isEnabled()) {
            return PushResult.SKIPPED;
        }

        PushSubscription subscription = destination.getPushSubscription();
        if (subscription == null) {
            log.warn("Registro WEB {} sem inscrição gravada", destination.getId());
            return PushResult.GONE;
        }

        try {
            String payload = objectMapper.writeValueAsString(buildPayload(message));

            var notification = new nl.martijndwars.webpush.Notification(
                    new Subscription(subscription.endpoint(),
                            new Subscription.Keys(subscription.keys().p256dh(), subscription.keys().auth())),
                    payload);

            int status = pushService.send(notification).getStatusLine().getStatusCode();

            // 404/410 é a forma que o protocolo tem de dizer que a inscrição
            // morreu: aba fechada para sempre, permissão revogada, perfil limpo.
            if (status == 404 || status == 410) {
                log.info("Inscrição Web Push {} expirou (HTTP {})", destination.getId(), status);
                return PushResult.GONE;
            }
            if (status >= 200 && status < 300) {
                return PushResult.DELIVERED;
            }

            log.warn("Serviço de push recusou o envio para o registro {}: HTTP {}",
                    destination.getId(), status);
            return PushResult.FAILED;
        } catch (InterruptedException e) {
            // Restaura o sinal para quem estiver desligando o pool de envio.
            Thread.currentThread().interrupt();
            return PushResult.FAILED;
        } catch (Exception e) {
            log.warn("Falha ao enviar Web Push para o registro {}: {}", destination.getId(), e.getMessage());
            return PushResult.FAILED;
        }
    }

    /** Formato consumido pelo {@code push} do service worker em /sw.js. */
    private Map<String, Object> buildPayload(PushMessage message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", message.title());
        payload.put("body", message.body());
        payload.put("url", message.url());
        payload.put("tag", message.tag());
        payload.put("data", message.data());
        return payload;
    }
}
