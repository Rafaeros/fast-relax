package br.rafaeros.fastrelax_api.features.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequestDTO(
    @NotBlank(message = "O refresh token é obrigatório")
    String refreshToken
) {}
