package br.rafaeros.fastrelax_api.features.notifications.push;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;

import br.rafaeros.fastrelax_api.features.notifications.DeviceToken;
import jakarta.annotation.PostConstruct;

/**
 * Entrega para Android e iOS via Firebase Cloud Messaging.
 *
 * <p>
 * O app empacotado com Capacitor pede a permissão, recebe o token do Firebase e
 * o registra em {@code POST /notifications/devices}. Daqui em diante o backend
 * só precisa do token: quem entrega ao aparelho é o FCM.
 *
 * <p>
 * Sem credencial configurada o provedor fica inerte — a notificação continua
 * sendo gravada e aparece na central do app, só não vira push. Isso permite
 * rodar o sistema inteiro antes de existir um projeto no Firebase.
 */
@Component
public class FcmPushProvider implements PushProvider {

    private static final Logger log = LoggerFactory.getLogger(FcmPushProvider.class);

    /** Nome próprio: o app default do Firebase pode ser usado por outra coisa. */
    private static final String APP_NAME = "fastrelax";

    @Value("${app.push.fcm.credentials-file:}")
    private String credentialsFile;

    private FirebaseApp firebaseApp;

    @PostConstruct
    void init() {
        if (credentialsFile == null || credentialsFile.isBlank()) {
            log.info("FCM desligado: defina app.push.fcm.credentials-file para habilitar push Android/iOS");
            return;
        }

        try (InputStream credentials = new FileInputStream(credentialsFile)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentials))
                    .build();

            // Reaproveita o app se o contexto for recarregado (devtools, testes):
            // inicializar duas vezes com o mesmo nome lança IllegalStateException.
            firebaseApp = FirebaseApp.getApps().stream()
                    .filter(app -> APP_NAME.equals(app.getName()))
                    .findFirst()
                    .orElseGet(() -> FirebaseApp.initializeApp(options, APP_NAME));

            log.info("FCM habilitado a partir de {}", credentialsFile);
        } catch (IOException | RuntimeException e) {
            // Credencial inválida não pode impedir a API de subir: sem push o
            // sistema perde um aviso, sem a API ele perde tudo.
            log.error("Falha ao inicializar o FCM com {}: {}", credentialsFile, e.getMessage());
        }
    }

    @Override
    public boolean supports(DeviceToken.Platform platform) {
        return platform == DeviceToken.Platform.ANDROID || platform == DeviceToken.Platform.IOS;
    }

    @Override
    public boolean isEnabled() {
        return firebaseApp != null;
    }

    @Override
    public PushResult send(DeviceToken destination, PushMessage message) {
        if (!isEnabled()) {
            return PushResult.SKIPPED;
        }

        Map<String, String> data = new HashMap<>(message.data());
        if (message.url() != null) {
            data.put("url", message.url());
        }

        // Sem o bloco `notification` do FCM, o Android só entrega ao app aberto.
        // Com ele, o sistema mostra o aviso mesmo com o app fechado — que é o
        // caso de uso inteiro do lembrete de massagem.
        Message fcmMessage = Message.builder()
                .setToken(destination.getToken())
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(message.title())
                        .setBody(message.body())
                        .build())
                .putAllData(data)
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setCollapseKey(message.tag())
                        .build())
                .build();

        try {
            FirebaseMessaging.getInstance(firebaseApp).send(fcmMessage);
            return PushResult.DELIVERED;
        } catch (FirebaseMessagingException e) {
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED
                    || e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                // App desinstalado ou token rotacionado: não adianta insistir.
                log.info("Token FCM {} não vale mais ({})", destination.getId(), e.getMessagingErrorCode());
                return PushResult.GONE;
            }
            log.warn("Falha temporária ao enviar push FCM para o registro {}: {}",
                    destination.getId(), e.getMessage());
            return PushResult.FAILED;
        }
    }
}
