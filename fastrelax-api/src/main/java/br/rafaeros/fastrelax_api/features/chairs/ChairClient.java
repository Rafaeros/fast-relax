package br.rafaeros.fastrelax_api.features.chairs;

import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Único ponto de comunicação com o ESP32.
 *
 * <p>
 * Toda chamada ao hardware passa por aqui — timeout, autenticação e tratamento
 * de falha ficam em um lugar só. Quem orquestra sessão não sabe que existe HTTP,
 * e trocar para MQTT depois mexe apenas nesta classe.
 */
@Component
public class ChairClient {

    private static final Logger log = LoggerFactory.getLogger(ChairClient.class);

    private final RestClient restClient;
    private final String deviceToken;
    private final int startDelaySeconds;

    public ChairClient(
            @Value("${app.chair.request-timeout-ms:3000}") int requestTimeoutMs,
            @Value("${app.chair.device-token:}") String deviceToken,
            @Value("${app.chair.start-delay-seconds:5}") int startDelaySeconds) {
        this.deviceToken = deviceToken;
        this.startDelaySeconds = startDelaySeconds;

        // Timeout curto de propósito: o colaborador está esperando na frente da
        // cadeira, e uma requisição pendurada seria pior que um erro imediato.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(requestTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(requestTimeoutMs));

        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /**
     * Liga o relé. O ESP32 recebe a duração e conta localmente, então ele desliga
     * sozinho mesmo que a rede caia ou o servidor reinicie.
     *
     * @return true quando o ESP32 confirmou o comando
     */
    public boolean start(Chair chair, Long sessionId, int durationSeconds) {
        return send(chair, "/start", Map.of(
                "sessionId", sessionId,
                "durationSeconds", durationSeconds,
                "startDelaySeconds", startDelaySeconds));
    }

    /**
     * Desliga o relé antes do fim previsto. Idempotente do lado do ESP32: parar
     * uma cadeira já parada não é erro.
     */
    public boolean stop(Chair chair, Long sessionId) {
        return send(chair, "/stop", Map.of("sessionId", sessionId));
    }

    /**
     * Aciona o relé por alguns segundos, sem sessão.
     *
     * <p>
     * Diagnóstico de instalação: se o relé clica e a cadeira liga, um
     * agendamento que falhou tem problema de software, não de fiação. O ESP32
     * desliga sozinho ao fim da duração.
     */
    public boolean testRelay(Chair chair, int durationSeconds) {
        return send(chair, "/relay-test", Map.of("durationSeconds", durationSeconds));
    }

    private boolean send(Chair chair, String path, Map<String, Object> body) {
        if (chair.getIpAddress() == null) {
            log.warn("Cadeira {} sem IP conhecido; comando {} não enviado", chair.getName(), path);
            return false;
        }
        String url = chair.baseUrl() + path;
        try {
            restClient.post()
                    .uri(url)
                    .header("X-Device-Token", deviceToken)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Comando {} enviado para a cadeira {} ({})", path, chair.getName(), url);
            return true;
        } catch (Exception e) {
            // Falha de hardware ou de rede não deve derrubar a requisição do
            // usuário: quem chamou decide o que fazer com o false.
            log.error("Falha ao enviar {} para a cadeira {} ({}): {}", path, chair.getName(), url,
                    e.getMessage());
            return false;
        }
    }
}
