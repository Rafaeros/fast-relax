package br.rafaeros.fastrelax_api.features.chairs.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cadastro de cadeira, exclusivo da equipe da plataforma.
 *
 * <p>
 * Quem instala o equipamento escolhe a empresa dona dele — por isso
 * {@code companyId} é obrigatório aqui e não existe em {@link SaveChairRequestDTO},
 * que é a edição, restrita à mesma equipe e sem reatribuição de empresa.
 */
public record CreateChairRequestDTO(
    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 2, max = 100, message = "O nome deve ter entre 2 e 100 caracteres")
    String name,

    @NotBlank(message = "O MAC address é obrigatório")
    @Pattern(regexp = "^([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}$",
            message = "MAC address inválido. Use o formato AA:BB:CC:DD:EE:FF")
    String macAddress,

    @NotNull(message = "A empresa é obrigatória")
    Long companyId,

    /** Opcional: o heartbeat preenche assim que o ESP32 se anunciar. */
    String ipAddress,

    @Min(value = 1, message = "Porta inválida")
    @Max(value = 65535, message = "Porta inválida")
    Integer port,

    /** Versão instalada no dispositivo. Opcional: nem toda cadeira passou pela atualização formal. */
    Long firmwareId,

    /**
     * Ponto de acesso em que esta cadeira deve entrar, dentro do SSID da
     * empresa. Em branco deixa o ESP32 escolher o de melhor sinal.
     */
    @Pattern(regexp = "^(|([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2})$",
            message = "BSSID inválido. Use o formato AA:BB:CC:DD:EE:FF")
    String wifiBssid,

    /** Broker MQTT específico desta cadeira. Em branco usa o padrão global. */
    String mqttHost,

    @Min(value = 1, message = "Porta inválida")
    @Max(value = 65535, message = "Porta inválida")
    Integer mqttPort,

    String mqttUsername,

    String mqttPassword
) {}
