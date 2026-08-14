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
    Integer port
) {}
