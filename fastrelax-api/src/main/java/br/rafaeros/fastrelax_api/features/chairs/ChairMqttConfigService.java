package br.rafaeros.fastrelax_api.features.chairs;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.rafaeros.fastrelax_api.core.crypto.CryptoService;
import br.rafaeros.fastrelax_api.core.exceptions.ResourceNotFoundException;
import br.rafaeros.fastrelax_api.features.chairs.dtos.ChairMqttResultDTO;
import br.rafaeros.fastrelax_api.features.chairs.mqtt.ChairMqttProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Entrega o broker MQTT às cadeiras.
 *
 * <p>
 * Espelha {@link ChairNetworkService}: separado do {@link ChairService}
 * porque a responsabilidade aqui é falar com o dispositivo, não cuidar do
 * cadastro.
 *
 * <p>
 * Diferente do Wi-Fi, a maioria das cadeiras nunca precisa disto — o broker
 * padrão ({@code app.mqtt.device-*}) já bate com o que o firmware traz
 * embutido em config.h. Este serviço só importa para quem tem
 * {@link Chair#hasMqttOverride()}, e mesmo assim cada campo cai
 * individualmente no padrão global quando não foi sobrescrito.
 *
 * <p>
 * A senha só é decifrada aqui, no instante do envio — mesma regra do Wi-Fi.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChairMqttConfigService {

    private final ChairRepository chairRepository;
    private final ChairClient chairClient;
    private final CryptoService cryptoService;
    private final ChairMqttProperties mqttProperties;

    /**
     * Empurra o broker para uma cadeira.
     *
     * <p>
     * Sem escopo de empresa, igual {@link ChairNetworkService#push(Long)}: quem
     * chama é o SYSADMIN. A barreira de papel está no controller.
     */
    @Transactional
    public ChairMqttResultDTO push(Long chairId) {
        Chair chair = chairRepository.findById(Objects.requireNonNull(chairId))
                .orElseThrow(() -> new ResourceNotFoundException("Cadeira não encontrada"));

        String host = chair.hasMqttOverride() ? chair.getMqttHost() : mqttProperties.getDeviceHost();
        int port = chair.getMqttPort() != null ? chair.getMqttPort() : mqttProperties.getDevicePort();
        String username = hasText(chair.getMqttUsername())
                ? chair.getMqttUsername()
                : mqttProperties.getDeviceUsername();
        // Decifrada aqui e em nenhum outro lugar: o valor vive o tempo desta
        // chamada e vai direto para a NVS do dispositivo.
        String password = chair.getMqttPasswordEncrypted() != null
                ? cryptoService.decrypt(chair.getMqttPasswordEncrypted())
                : mqttProperties.getDevicePassword();

        ChairCommandResult result = chairClient.pushMqtt(chair, host, port, username, password);

        if (result.isAccepted()) {
            // Marca o envio, não a aplicação: diferente do Wi-Fi, o firmware não
            // relata o broker em uso no heartbeat, então não há como confirmar que
            // a cadeira de fato reconectou no novo — só que aceitou gravar.
            chair.setMqttSyncedAt(LocalDateTime.now());
            chairRepository.save(chair);
        } else {
            log.warn("Configuração de MQTT não entregue à cadeira {}: {}",
                    chair.getName(), result.outcome());
        }

        return new ChairMqttResultDTO(
                chair.getId(),
                chair.getName(),
                result.isAccepted(),
                result.outcome().name(),
                messageFor(result));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /** Mesma mensagem de {@link ChairNetworkService}, adaptada ao broker. */
    private String messageFor(ChairCommandResult result) {
        return switch (result.outcome()) {
            case ACCEPTED -> "Configuração gravada. A cadeira vai reconectar no broker novo.";
            case NO_ADDRESS -> "A cadeira nunca se anunciou: sem IP conhecido, não há como alcançá-la.";
            case UNREACHABLE -> "A cadeira não respondeu. Confira se ela está ligada e na rede atual.";
            default -> "A cadeira recusou a configuração.";
        };
    }
}
