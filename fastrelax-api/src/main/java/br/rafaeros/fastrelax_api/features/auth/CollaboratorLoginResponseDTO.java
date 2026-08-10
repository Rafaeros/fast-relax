package br.rafaeros.fastrelax_api.features.auth;

public record CollaboratorLoginResponseDTO(
    String token,
    String refreshToken,
    long expiresInSeconds,
    Long collaboratorId,
    String name
) {}
