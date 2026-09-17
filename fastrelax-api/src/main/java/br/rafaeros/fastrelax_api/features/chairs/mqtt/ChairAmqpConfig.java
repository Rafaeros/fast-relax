package br.rafaeros.fastrelax_api.features.chairs.mqtt;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;

/**
 * Fila e bindings do backend na exchange {@code amq.topic} do RabbitMQ —
 * a mesma exchange que o plugin {@code rabbitmq_mqtt} usa para traduzir entre
 * MQTT e AMQP. Não declara a exchange em si: ela já existe em todo vhost
 * (é uma das {@code amq.*} padrão do RabbitMQ), e redeclará-la à toa arrisca
 * um conflito de propriedades bobo. Aqui a referência a {@code TopicExchange}
 * é só para o {@link BindingBuilder} montar o nome — não é bean, então o
 * {@code RabbitAdmin} não tenta declará-la.
 *
 * <p>
 * Três bindings, um por assunto que o firmware publica (status, online, ack).
 * O {@code *} do padrão AMQP casa exatamente um segmento — o MAC do
 * dispositivo, sem pontos.
 *
 * <p>
 * Só existe quando {@code app.mqtt.enabled=true}, mesma condição do
 * {@link ChairMqttGateway}: desligado, nada aqui declara fila nem conecta ao
 * broker.
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.mqtt", name = "enabled", havingValue = "true")
public class ChairAmqpConfig {

    private static final String EXCHANGE = "amq.topic";

    private final ChairMqttProperties properties;

    @Bean
    Queue chairEventsQueue() {
        return new Queue(properties.getQueueName(), true);
    }

    @Bean
    Binding chairStatusBinding(Queue chairEventsQueue) {
        return bind(chairEventsQueue, "status");
    }

    @Bean
    Binding chairOnlineBinding(Queue chairEventsQueue) {
        return bind(chairEventsQueue, "online");
    }

    @Bean
    Binding chairAckBinding(Queue chairEventsQueue) {
        return bind(chairEventsQueue, "ack");
    }

    private Binding bind(Queue queue, String subtopic) {
        String pattern = properties.getTopicPrefix().replace('/', '.') + ".*." + subtopic;
        return BindingBuilder.bind(queue).to(new TopicExchange(EXCHANGE)).with(pattern);
    }
}
