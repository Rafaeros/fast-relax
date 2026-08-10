package br.rafaeros.fastrelax_api.features.collaborators.dtos;

import br.rafaeros.fastrelax_api.features.collaborators.Collaborator;
import java.time.LocalDateTime;

public record CollaboratorResponseDTO(
    Long id,
    Long departmentId,
    String departmentName,
    String name,
    String cpf,
    String phoneNumber,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime deletedAt
) {
    /**
     * @param cpf already decrypted by the service — the entity only holds ciphertext,
     *            so it cannot resolve this on its own.
     */
    public CollaboratorResponseDTO(Collaborator entity, String cpf) {
        this(
            entity.getId(),
            entity.getDepartment() != null ? entity.getDepartment().getId() : null,
            entity.getDepartment() != null ? entity.getDepartment().getName() : null,
            entity.getName(),
            cpf,
            entity.getPhoneNumber(),
            entity.isActive(),
            entity.getCreatedAt(),
            entity.getDeletedAt()
        );
    }
}
