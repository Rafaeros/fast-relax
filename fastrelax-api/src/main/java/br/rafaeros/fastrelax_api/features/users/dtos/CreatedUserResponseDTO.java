package br.rafaeros.fastrelax_api.features.users.dtos;

/**
 * Resposta do cadastro de usuário.
 *
 * <p>
 * {@code temporaryPassword} aparece <strong>somente aqui</strong>: é gerada, exibida
 * uma vez e guardada apenas como hash. Se o ADMIN perder o valor, não há como
 * recuperá-lo — só redefinir.
 */
public record CreatedUserResponseDTO(
    UserResponseDTO user,
    String temporaryPassword
) {}
