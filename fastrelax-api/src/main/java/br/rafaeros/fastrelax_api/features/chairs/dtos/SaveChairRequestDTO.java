package br.rafaeros.fastrelax_api.features.chairs.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cadastro e edição de cadeira pelo RH. */
public record SaveChairRequestDTO(
    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 2, max = 100, message = "O nome deve ter entre 2 e 100 caracteres")
    String name,

    @NotBlank(message = "O MAC address é obrigatório")
    @Pattern(regexp = "^([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}$",
            message = "MAC address inválido. Use o formato AA:BB:CC:DD:EE:FF")
    String macAddress,

    /** Opcional: o heartbeat preenche assim que o ESP32 se anunciar. */
    String ipAddress,

    @Min(value = 1, message = "Porta inválida")
    @Max(value = 65535, message = "Porta inválida")
    Integer port
) {}
