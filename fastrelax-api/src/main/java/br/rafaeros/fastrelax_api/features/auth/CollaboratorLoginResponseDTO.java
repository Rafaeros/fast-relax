package br.rafaeros.fastrelax_api.features.auth;

/**
 * @param mustChangePassword verdadeiro logo após o cadastro: o app deve levar
 *                           direto à definição de senha, porque o resto da API
 *                           fica bloqueado até lá
 */
public record CollaboratorLoginResponseDTO(
    String token,
    String refreshToken,
    long expiresInSeconds,
    Long collaboratorId,
    String name,
    Long companyId,
    String companyName,
    boolean mustChangePassword
) {}
