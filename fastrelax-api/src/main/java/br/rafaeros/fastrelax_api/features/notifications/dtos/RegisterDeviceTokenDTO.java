package br.rafaeros.fastrelax_api.features.notifications.dtos;

import br.rafaeros.fastrelax_api.features.notifications.DeviceToken;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterDeviceTokenDTO(
    @NotBlank(message = "O token do dispositivo é obrigatório")
    String token,

    @NotNull(message = "A plataforma é obrigatória")
    DeviceToken.Platform platform
) {}
