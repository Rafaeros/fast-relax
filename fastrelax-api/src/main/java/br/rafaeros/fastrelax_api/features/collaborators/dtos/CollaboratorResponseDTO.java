package br.rafaeros.fastrelax_api.features.collaborators.dtos;

import java.time.LocalDateTime;

import br.rafaeros.fastrelax_api.features.collaborators.Collaborator;

public record CollaboratorResponseDTO(
    Long id,
    Long departmentId,
    String departmentName,
    String name,
    String cpf,
    String phoneNumber,
    /** Nulo quando não foi informado; sem ele não há recuperação de senha. */
    String email,
    /** Verdadeiro enquanto o colaborador ainda usa a senha temporária do RH. */
    boolean mustChangePassword,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime deletedAt
) {
    /**
     * @param cpf já decifrado pelo serviço — a entidade só guarda o ciphertext,
     *            então não tem como resolver isto sozinha
     */
    public CollaboratorResponseDTO(Collaborator entity, String cpf) {
        this(
            entity.getId(),
            entity.getDepartment() != null ? entity.getDepartment().getId() : null,
            entity.getDepartment() != null ? entity.getDepartment().getName() : null,
            entity.getName(),
            cpf,
            entity.getPhoneNumber(),
            entity.getEmail(),
            entity.isMustChangePassword(),
            entity.isActive(),
            entity.getCreatedAt(),
            entity.getDeletedAt()
        );
    }
}
