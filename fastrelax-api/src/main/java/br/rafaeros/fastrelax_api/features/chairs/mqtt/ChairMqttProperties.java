package br.rafaeros.fastrelax_api.features.chairs.mqtt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Espelha, no lado do backend, o bloco {@code MQTT} do {@code config.h} do
 * firmware — só que aqui é a topologia AMQP que fala com o mesmo broker, não
 * um cliente MQTT. {@code app.mqtt.enabled=false} é o padrão dos dois lados:
 * até uma cadeira de verdade falar MQTT, ligar isto aqui não muda nada.
 *
 * <p>
 * Host, porta e credenciais do broker não aparecem aqui: ficam nas
 * propriedades padrão {@code spring.rabbitmq.*}, que o Spring Boot já
 * autoconfigura.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.mqtt")
public class ChairMqttProperties {

    private boolean enabled = false;

    /** Precisa bater com MQTT_TOPIC_PREFIX no config.h — com barra; a conversão para ponto é interna. */
    private String topicPrefix = "fastrelax/chairs";

    /**
     * Fila do backend, ligada por três bindings (status/online/ack) na exchange
     * {@code amq.topic}. Independente do prefixo dos tópicos — é só o nome
     * interno da fila no broker.
     */
    private String queueName = "fastrelax.chairs.backend";

    /** Quanto esperar pelo ack de um comando antes de UNREACHABLE. */
    private int commandTimeoutMs = 5000;

    /**
     * Broker padrão que as cadeiras (ESP32) falam — MQTT puro, porta 1883.
     *
     * <p>
     * Diferente de {@code spring.rabbitmq.*} acima: aquilo é o lado AMQP que o
     * <em>backend</em> usa para falar com o broker (porta 5672); isto é o que o
     * <em>firmware</em> usa via {@code PubSubClient}. Mesmo broker físico na
     * configuração usual (plugin {@code rabbitmq_mqtt}), só a porta muda — por
     * isso host/usuário/senha aqui reaproveitam as mesmas variáveis de ambiente
     * MQTT_HOST/MQTT_USERNAME/MQTT_PASSWORD do {@code spring.rabbitmq.*}, e só
     * a porta tem variável própria.
     *
     * <p>
     * É o que {@link br.rafaeros.fastrelax_api.features.chairs.ChairMqttConfigService}
     * usa como padrão para uma cadeira sem override próprio — o caso comum,
     * que nunca precisa de POST /mqtt nenhum porque já bate com o que o
     * firmware traz embutido em config.h.
     */
    private String deviceHost;
    private int devicePort = 1883;
    private String deviceUsername;
    private String devicePassword;
}
