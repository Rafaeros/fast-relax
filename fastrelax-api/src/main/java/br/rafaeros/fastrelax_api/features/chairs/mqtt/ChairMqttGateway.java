package br.rafaeros.fastrelax_api.features.chairs.mqtt;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.rafaeros.fastrelax_api.core.crypto.CryptoService;
import br.rafaeros.fastrelax_api.features.chairs.Chair;
import br.rafaeros.fastrelax_api.features.chairs.ChairCommandResult;
import br.rafaeros.fastrelax_api.features.chairs.ChairCommandResult.Outcome;
import br.rafaeros.fastrelax_api.features.chairs.ChairRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Transporte dos comandos de cadeira via RabbitMQ — a contrapartida do
 * {@code mqtt_client.cpp} do firmware, só que falando AMQP nativo em vez de
 * MQTT.
 *
 * <p>
 * O firmware é PubSubClient (MQTT puro); o backend não precisa de um cliente
 * MQTT porque o plugin {@code rabbitmq_mqtt} do broker já traduz os dois
 * mundos pela exchange padrão {@code amq.topic} — o tópico
 * {@code fastrelax/chairs/<mac>/status} chega ao backend como routing key
 * {@code fastrelax.chairs.<mac>.status} (barra por ponto), e uma publicação
 * AMQP na mesma exchange com routing key {@code ....cmd.start} sai do outro
 * lado como MQTT PUBLISH em {@code fastrelax/chairs/<mac>/cmd/start} — é o
 * que a cadeira está de fato assinada. Ver `ChairAmqpConfig` para a fila e os
 * bindings.
 *
 * <p>
 * Só existe como bean quando {@code app.mqtt.enabled=true} — ver
 * {@link ConditionalOnProperty}. Desligado (padrão), nenhuma fila é
 * declarada e nenhuma conexão ao RabbitMQ é aberta; {@code ChairClient} nem
 * enxerga este componente (injeção por {@code Optional}).
 *
 * <p>
 * Correlação do pedido/resposta: o firmware processa um comando por vez e
 * publica o desfecho em {@code .../ack}, sem id de correlação. A suposição —
 * segura porque {@code ChairCommandService} já serializa os comandos por
 * cadeira — é que existe no máximo um comando em voo por MAC; por isso a fila
 * de pendências é chaveada só pelo device id.
 *
 * <p>
 * Não depende de {@code ChairService} de propósito: isso fecharia um ciclo
 * (ChairService -> ChairClient -> este gateway -> ChairService). Presença via
 * {@code status}/{@code online} é aplicada direto no {@link ChairRepository}.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.mqtt", name = "enabled", havingValue = "true")
public class ChairMqttGateway {

    private static final String EXCHANGE = "amq.topic";

    private final ChairMqttProperties properties;
    private final ChairRepository chairRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final CryptoService cryptoService;
    private final int cooldownSeconds;

    private final Map<String, CompletableFuture<JsonNode>> pending = new ConcurrentHashMap<>();

    public ChairMqttGateway(ChairMqttProperties properties, ChairRepository chairRepository,
            RabbitTemplate rabbitTemplate, ObjectMapper objectMapper, CryptoService cryptoService,
            @Value("${app.chair.cooldown-seconds:40}") int cooldownSeconds) {
        this.properties = properties;
        this.chairRepository = chairRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.cryptoService = cryptoService;
        this.cooldownSeconds = cooldownSeconds;
        log.info("mqtt: transporte AMQP ativo (exchange {}, fila {})", EXCHANGE, properties.getQueueName());
    }

    public ChairCommandResult start(Chair chair, Long sessionId, int durationSeconds, int startDelaySeconds) {
        return send(chair, "start", Map.of(
                "sessionId", sessionId,
                "durationSeconds", durationSeconds,
                "startDelaySeconds", startDelaySeconds));
    }

    public ChairCommandResult stop(Chair chair, Long sessionId) {
        return send(chair, "stop", Map.of("sessionId", sessionId));
    }

    public ChairCommandResult testRelay(Chair chair, int durationSeconds) {
        return send(chair, "relay-test", Map.of("durationSeconds", durationSeconds));
    }

    public ChairCommandResult pushNetwork(Chair chair, String ssid, String password, String bssid) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ssid", ssid);
        body.put("password", password == null ? "" : password);
        body.put("bssid", bssid == null ? "" : bssid);
        return send(chair, "network", body);
    }

    public ChairCommandResult pushMqtt(Chair chair, String host, int port, String username, String password) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("host", host);
        body.put("port", port);
        body.put("username", username == null ? "" : username);
        body.put("password", password == null ? "" : password);
        return send(chair, "mqtt", body);
    }

    public ChairCommandResult pushPower(Chair chair, boolean active) {
        return send(chair, "power", Map.of("active", active));
    }

    // ---- publicação de comando + espera do ack ----

    private ChairCommandResult send(Chair chair, String subtopic, Map<String, Object> body) {
        String deviceId = deviceId(chair);
        String routingKey = dottedPrefix() + "." + deviceId + ".cmd." + subtopic;

        // Token desta cadeira, não um segredo único: cada ESP32 gerou o próprio
        // no primeiro boot e pareou com o backend no primeiro heartbeat.
        String deviceToken = chair.getDeviceTokenEncrypted() == null ? ""
                : cryptoService.decrypt(chair.getDeviceTokenEncrypted());

        Map<String, Object> withToken = new LinkedHashMap<>(body);
        withToken.put("token", deviceToken);

        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        pending.put(deviceId, future);
        try {
            String json = objectMapper.writeValueAsString(withToken);
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, json);

            JsonNode ack = future.get(properties.getCommandTimeoutMs(), TimeUnit.MILLISECONDS);
            log.info("mqtt: comando {} confirmado pela cadeira {} ({})", subtopic, chair.getName(), routingKey);
            return translateAck(ack);

        } catch (TimeoutException e) {
            log.warn("mqtt: cadeira {} não respondeu a {} em {}ms", chair.getName(), subtopic,
                    properties.getCommandTimeoutMs());
            return ChairCommandResult.of(Outcome.UNREACHABLE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ChairCommandResult.of(Outcome.UNREACHABLE);
        } catch (Exception e) {
            // Broker fora do ar, credencial inválida, exchange inacessível — a
            // publicação em si falhou, sem chegar a esperar ack nenhum.
            log.error("mqtt: falha ao publicar {} para {} ({}): {}", subtopic, chair.getName(), routingKey,
                    e.getMessage());
            return ChairCommandResult.of(Outcome.UNREACHABLE);
        } finally {
            pending.remove(deviceId, future);
        }
    }

    /**
     * Traduz o ack do firmware. Mesmo vocabulário do HTTP
     * ({@code ChairClient#readRefusal}), só que lido do campo {@code status} em
     * vez de {@code error} — o ack não distingue sucesso de recusa por status
     * HTTP, então o texto carrega os dois.
     */
    private ChairCommandResult translateAck(JsonNode ack) {
        String status = ack.path("status").asText("");
        return switch (status) {
            case "started", "stopped", "relay_test_started", "network_saved", "mqtt_saved", "power_applied" ->
                ChairCommandResult.accepted();
            case "cooling_down" -> ChairCommandResult.coolingDown(ack.path("remainingSeconds").asInt(0));
            case "session_in_progress" -> ChairCommandResult.of(Outcome.BUSY);
            case "invalid_duration", "ssid_required", "invalid_bssid", "host_required", "invalid_port" ->
                ChairCommandResult.of(Outcome.INVALID_REQUEST);
            case "unauthorized" -> ChairCommandResult.of(Outcome.UNAUTHORIZED);
            default -> ChairCommandResult.of(Outcome.INVALID_REQUEST);
        };
    }

    // ---- consumo (status / online / ack) ----

    /**
     * Uma fila só para os três assuntos: a routing key (último e penúltimo
     * segmento, separados por ponto) diz qual é. Ver `ChairAmqpConfig` para os
     * bindings que alimentam esta fila.
     */
    @RabbitListener(queues = "${app.mqtt.queue-name:fastrelax.chairs.backend}")
    public void onMessage(Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);

        String[] parts = routingKey.split("\\.");
        if (parts.length < 2) {
            log.debug("mqtt: routing key inesperada {}", routingKey);
            return;
        }
        String subtopic = parts[parts.length - 1];
        String deviceId = parts[parts.length - 2];

        switch (subtopic) {
            case "ack" -> completeAck(deviceId, payload);
            case "status" -> applyStatus(deviceId, payload);
            case "online" -> applyPresence(deviceId, payload);
            default -> log.debug("mqtt: routing key desconhecida {}", routingKey);
        }
    }

    private void completeAck(String deviceId, String payload) {
        CompletableFuture<JsonNode> future = pending.get(deviceId);
        if (future == null) {
            // Ack tardio (chegou depois do timeout) ou sem comando pendente daqui —
            // não há para onde entregar o resultado.
            return;
        }
        try {
            future.complete(objectMapper.readTree(payload));
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
    }

    /** Estado da sessão, publicado periodicamente — presença mais fina que o heartbeat HTTP. */
    private void applyStatus(String deviceId, String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            chairRepository.findByMacAddress(macFromDeviceId(deviceId)).ifPresent(chair -> {
                String ip = node.path("ip").asText(null);
                if (ip != null && !ip.isBlank()) {
                    chair.setIpAddress(ip);
                }
                chair.setLastSeenAt(LocalDateTime.now());
                String phase = node.hasNonNull("phase") ? node.path("phase").asText() : null;
                Integer remaining = node.hasNonNull("remainingSeconds") ? node.path("remainingSeconds").asInt()
                        : null;
                chair.applyPhase(phase, remaining, cooldownSeconds);
                chairRepository.save(chair);
            });
        } catch (Exception e) {
            log.warn("mqtt: status inválido de {}: {}", deviceId, e.getMessage());
        }
    }

    /**
     * "online"/"offline" — retido no MQTT, com Last Will. "offline" chega em
     * segundos depois de a cadeira sumir, bem mais rápido que os até
     * {@code app.chair.offline-after-seconds} do heartbeat HTTP, então zera
     * {@code lastSeenAt} para refletir a queda na hora.
     */
    private void applyPresence(String deviceId, String payload) {
        boolean online = "online".equalsIgnoreCase(payload.trim());
        chairRepository.findByMacAddress(macFromDeviceId(deviceId)).ifPresent(chair -> {
            chair.setLastSeenAt(online ? LocalDateTime.now() : null);
            chairRepository.save(chair);
        });
    }

    /** {@code app.mqtt.topic-prefix} com ponto no lugar de barra — a routing key AMQP. */
    private String dottedPrefix() {
        return properties.getTopicPrefix().replace('/', '.');
    }

    /** MAC em minúsculas, sem os dois-pontos — mesmo formato que o firmware usa nos tópicos. */
    private static String deviceId(Chair chair) {
        return chair.getMacAddress().replace(":", "").toLowerCase();
    }

    /** Inverso de {@link #deviceId}: recompõe o MAC no formato gravado no cadastro. */
    private static String macFromDeviceId(String deviceId) {
        String hex = deviceId.toUpperCase();
        StringBuilder sb = new StringBuilder(hex.length() + 5);
        for (int i = 0; i < hex.length(); i += 2) {
            if (i > 0) {
                sb.append(':');
            }
            sb.append(hex, i, Math.min(i + 2, hex.length()));
        }
        return sb.toString();
    }
}
