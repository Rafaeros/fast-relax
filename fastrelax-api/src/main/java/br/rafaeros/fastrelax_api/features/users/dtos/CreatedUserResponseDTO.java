package br.rafaeros.fastrelax_api.features.users.dtos;

import br.rafaeros.fastrelax_api.core.dto.CredentialDeliveryDTO;

/**
 * Resposta do cadastro de usuário.
 *
 * <p>
 * {@code credential} diz como o acesso foi entregue: convite por e-mail ou
 * senha temporária. Quando é senha, ela aparece <strong>somente aqui</strong> —
 * o banco guarda apenas o hash, e perder o valor significa ter de redefinir.
 */
public record CreatedUserResponseDTO(
    UserResponseDTO user,
    CredentialDeliveryDTO credential
) {}
