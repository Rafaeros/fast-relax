package br.rafaeros.fastrelax_api.features.departments.dtos;

import br.rafaeros.fastrelax_api.features.departments.Department;
import java.time.LocalDateTime;

public record DepartmentResponseDTO(
    Long id,
    String name,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime deletedAt
) {
    public DepartmentResponseDTO(Department entity) {
        this(
            entity.getId(),
            entity.getName(),
            entity.isActive(),
            entity.getCreatedAt(),
            entity.getDeletedAt()
        );
    }
}
