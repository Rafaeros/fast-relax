package br.rafaeros.fastrelax_api.features.chairs.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Batida periódica do ESP32.
 *
 * <p>
 * O MAC identifica; o IP é o endereço atual, que muda a cada renovação de DHCP e
 * por isso é reenviado sempre.
 */
public record ChairHeartbeatRequestDTO(
    @NotBlank(message = "O MAC address é obrigatório")
    @Pattern(regexp = "^([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}$",
            message = "MAC address inválido. Use o formato AA:BB:CC:DD:EE:FF")
    String macAddress,

    @NotBlank(message = "O IP é obrigatório")
    String ipAddress,

    /** Opcional: sem valor, mantém a porta já registrada. */
    @Min(value = 1, message = "Porta inválida")
    @Max(value = 65535, message = "Porta inválida")
    Integer port,

    /**
     * Fase da máquina de estados do firmware: {@code idle}, {@code starting},
     * {@code running}, {@code testing} ou {@code cooldown}.
     *
     * <p>
     * Opcional para não quebrar dispositivo com firmware anterior ao campo — sem
     * valor, o backend mantém o que já sabia sobre a cadeira.
     */
    String phase,

    /**
     * Segundos que faltam para o fim da fase atual. Em {@code cooldown} é o que
     * define até quando a cadeira recusa acionamento.
     */
    @Min(value = 0, message = "Tempo restante inválido")
    Integer remainingSeconds,

    /**
     * SSID em que o dispositivo está de fato conectado.
     *
     * <p>
     * É a confirmação de que a configuração enviada foi aplicada, e não só
     * recebida: sem isso, uma cadeira que gravou o SSID novo mas não conseguiu
     * entrar nele apareceria como configurada, e continuaria no ar pela rede
     * antiga até alguém desligar o AP velho.
     *
     * <p>
     * Opcional — firmware anterior ao campo simplesmente não o envia.
     */
    String ssid,

    /** Ponto de acesso fixado no dispositivo; vazio quando ele escolhe sozinho. */
    String bssid,

    /** Falso enquanto a cadeira ainda roda a rede de fábrica do config.h. */
    Boolean provisioned
) {

    /** Fase que o firmware informa enquanto a cadeira se estabiliza. */
    public static final String PHASE_COOLDOWN = "cooldown";

    public boolean isCoolingDown() {
        return PHASE_COOLDOWN.equalsIgnoreCase(phase);
    }
}
