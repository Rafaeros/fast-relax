package br.rafaeros.fastrelax_api.features.chairs;

import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.rafaeros.fastrelax_api.features.chairs.ChairCommandResult.Outcome;

/**
 * Único ponto de comunicação com o ESP32.
 *
 * <p>
 * Toda chamada ao hardware passa por aqui — timeout, autenticação e tratamento
 * de falha ficam em um lugar só. Quem orquestra sessão não sabe que existe HTTP,
 * e trocar para MQTT depois mexe apenas nesta classe.
 *
 * <p>
 * A resposta do dispositivo é traduzida em {@link ChairCommandResult} em vez de
 * um booleano: o firmware recusa comandos com um motivo no corpo, e o motivo
 * decide o que dizer a quem está esperando na frente da cadeira.
 */
@Component
public class ChairClient {

    private static final Logger log = LoggerFactory.getLogger(ChairClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String deviceToken;
    private final int startDelaySeconds;

    public ChairClient(
            @Value("${app.chair.request-timeout-ms:3000}") int requestTimeoutMs,
            @Value("${app.chair.device-token:}") String deviceToken,
            @Value("${app.chair.start-delay-seconds:5}") int startDelaySeconds,
            ObjectMapper objectMapper) {
        this.deviceToken = deviceToken;
        this.startDelaySeconds = startDelaySeconds;
        this.objectMapper = objectMapper;

        // Timeout curto de propósito: o colaborador está esperando na frente da
        // cadeira, e uma requisição pendurada seria pior que um erro imediato.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(requestTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(requestTimeoutMs));

        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /**
     * Liga a cadeira. O ESP32 recebe a duração e conta localmente, então ele
     * desliga sozinho mesmo que a rede caia ou o servidor reinicie.
     */
    public ChairCommandResult start(Chair chair, Long sessionId, int durationSeconds) {
        return send(chair, "/start", Map.of(
                "sessionId", sessionId,
                "durationSeconds", durationSeconds,
                "startDelaySeconds", startDelaySeconds));
    }

    /**
     * Desliga antes do fim previsto. Idempotente do lado do ESP32: parar uma
     * cadeira já parada não é erro.
     */
    public ChairCommandResult stop(Chair chair, Long sessionId) {
        return send(chair, "/stop", Map.of("sessionId", sessionId));
    }

    /**
     * Liga e desliga a cadeira, sem sessão.
     *
     * <p>
     * Diagnóstico de instalação: se o relé clica e a cadeira liga, um
     * agendamento que falhou tem problema de software, não de fiação. O ESP32
     * desliga sozinho ao fim da duração.
     */
    public ChairCommandResult testRelay(Chair chair, int durationSeconds) {
        return send(chair, "/relay-test", Map.of("durationSeconds", durationSeconds));
    }

    /**
     * Grava SSID, senha e BSSID na NVS do ESP32.
     *
     * <p>
     * É o que substitui recompilar o firmware com a rede do cliente e ir até
     * cada cadeira regravar a placa.
     *
     * <p>
     * O dispositivo responde <em>antes</em> de reconectar, de propósito: a
     * conversa está acontecendo pela rede que a troca vai derrubar, e reconectar
     * primeiro deixaria esta requisição sem resposta — o backend não saberia se
     * a configuração chegou.
     *
     * @param bssid opcional; vazio deixa o ESP32 escolher o AP de melhor sinal
     */
    public ChairCommandResult pushNetwork(Chair chair, String ssid, String password, String bssid) {
        // O firmware distingue campo ausente de campo vazio: string vazia
        // significa "sem fixação de AP", e é o que apaga um BSSID configurado
        // antes. Mandar null faria o ESP32 manter o valor anterior.
        return send(chair, "/network", Map.of(
                "ssid", ssid,
                "password", password == null ? "" : password,
                "bssid", bssid == null ? "" : bssid));
    }

    /**
     * Liga/desliga o relé de corte de energia do painel, conforme o {@code
     * active} do cadastro da cadeira.
     *
     * <p>
     * Best-effort: se a cadeira estiver offline agora, este envio falha e quem
     * chamou não deve tratar isso como erro do toggle — o próximo heartbeat
     * que o ESP32 conseguir mandar já reconcilia sozinho, lendo o mesmo campo
     * {@code active} que volta na resposta.
     */
    public ChairCommandResult pushPower(Chair chair, boolean active) {
        return send(chair, "/power", Map.of("active", active));
    }

    private ChairCommandResult send(Chair chair, String path, Map<String, Object> body) {
        if (chair.getIpAddress() == null) {
            log.warn("Cadeira {} sem IP conhecido; comando {} não enviado", chair.getName(), path);
            return ChairCommandResult.of(Outcome.NO_ADDRESS);
        }
        String url = chair.baseUrl() + path;
        try {
            // O JSON é serializado aqui, e não entregue como Map, por causa do
            // ESP32: passando o objeto, o Spring não sabe o tamanho final e
            // envia em `Transfer-Encoding: chunked`, sem `Content-Length`. O
            // WebServer do ESP32 lê o corpo pelo Content-Length — sem ele,
            // `server.arg("plain")` chega vazio e o firmware recusa o comando.
            String json = objectMapper.writeValueAsString(body);

            ResponseEntity<String> response = restClient.post()
                    .uri(url)
                    .header("X-Device-Token", deviceToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json)
                    .retrieve()
                    // Sem isto o RestClient lança em 4xx, e a exceção apagaria a
                    // diferença entre "recusou com motivo" e "não respondeu" —
                    // que é justamente o que interessa aqui.
                    .onStatus(HttpStatusCode::isError, (request, errorResponse) -> {
                    })
                    .toEntity(String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Comando {} aceito pela cadeira {} ({})", path, chair.getName(), url);
                return ChairCommandResult.accepted();
            }

            ChairCommandResult refusal = readRefusal(response);
            log.warn("Cadeira {} recusou {}: {} ({})", chair.getName(), path, refusal.outcome(),
                    response.getStatusCode());
            return refusal;

        } catch (ResourceAccessException e) {
            // Timeout, conexão recusada, host inalcançável: o dispositivo não
            // respondeu nada. Rede caída ou firmware travado — o estado da
            // cadeira fica desconhecido, e é isso que UNREACHABLE significa.
            log.error("Cadeira {} não respondeu a {} ({}): {}", chair.getName(), path, url,
                    e.getMessage());
            return ChairCommandResult.of(Outcome.UNREACHABLE);

        } catch (Exception e) {
            // Falha de hardware ou de rede não deve derrubar a requisição do
            // usuário: quem chamou decide o que fazer com o resultado.
            log.error("Falha ao enviar {} para a cadeira {} ({}): {}", path, chair.getName(), url,
                    e.getMessage());
            return ChairCommandResult.of(Outcome.UNREACHABLE);
        }
    }

    /**
     * Traduz a recusa do firmware.
     *
     * <p>
     * O corpo vem como {@code {"error":"cooling_down","remainingSeconds":27}}. O
     * código de erro manda; o status HTTP só decide quando o corpo não veio ou
     * não pôde ser lido — dispositivo antigo, resposta truncada.
     */
    private ChairCommandResult readRefusal(ResponseEntity<String> response) {
        String body = response.getBody();

        if (body != null && !body.isBlank()) {
            try {
                JsonNode node = objectMapper.readTree(body);
                String error = node.path("error").asText("");

                switch (error) {
                    case "cooling_down":
                        return ChairCommandResult.coolingDown(node.path("remainingSeconds").asInt(0));
                    case "session_in_progress":
                        return ChairCommandResult.of(Outcome.BUSY);
                    case "invalid_duration":
                        return ChairCommandResult.of(Outcome.INVALID_REQUEST);
                    case "unauthorized":
                        return ChairCommandResult.of(Outcome.UNAUTHORIZED);
                    default:
                        break;
                }
            } catch (Exception e) {
                log.warn("Resposta de erro da cadeira em formato inesperado: {}", body);
            }
        }

        int status = response.getStatusCode().value();
        if (status == 401) {
            return ChairCommandResult.of(Outcome.UNAUTHORIZED);
        }
        if (status == 409) {
            return ChairCommandResult.of(Outcome.BUSY);
        }
        return ChairCommandResult.of(Outcome.INVALID_REQUEST);
    }
}
