package br.rafaeros.fastrelax_api.features.auth;

/**
 * @param token              JWT de acesso, vida curta
 * @param refreshToken       token opaco de vida longa, usado para renovar o acesso
 * @param expiresInSeconds   validade do token de acesso, para o cliente renovar antes de expirar
 * @param mustChangePassword quando verdadeiro, o app deve levar direto à tela de
 *                           definição de senha: o restante da API fica bloqueado
 *                           até que ela seja definida
 */
public record LoginResponseDTO(
    String token,
    String refreshToken,
    long expiresInSeconds,
    boolean mustChangePassword
) {}
