package br.rafaeros.fastrelax_api.features.collaborators.dtos;

import br.rafaeros.fastrelax_api.core.dto.CredentialDeliveryDTO;

/**
 * Resposta do cadastro de colaborador.
 *
 * <p>
 * {@code credential} diz como o acesso foi entregue: convite por e-mail, quando
 * há endereço cadastrado, ou senha temporária para o RH repassar. Quando é
 * senha, ela aparece <strong>somente aqui</strong> — o banco guarda apenas o
 * hash, e perder o valor significa ter de redefinir.
 */
public record CreatedCollaboratorResponseDTO(
    CollaboratorResponseDTO collaborator,
    CredentialDeliveryDTO credential
) {}
