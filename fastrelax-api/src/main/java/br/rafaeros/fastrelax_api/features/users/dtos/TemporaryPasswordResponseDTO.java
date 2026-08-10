package br.rafaeros.fastrelax_api.features.users.dtos;

/**
 * Senha temporária gerada numa redefinição.
 *
 * <p>
 * Exibida uma única vez: o banco guarda apenas o hash, então perder este valor
 * significa ter de redefinir de novo.
 */
public record TemporaryPasswordResponseDTO(
    String temporaryPassword
) {}
