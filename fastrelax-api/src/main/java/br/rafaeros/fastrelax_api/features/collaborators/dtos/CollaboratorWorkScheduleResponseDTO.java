package br.rafaeros.fastrelax_api.features.collaborators.dtos;

import br.rafaeros.fastrelax_api.features.collaborators.CollaboratorWorkSchedule;
import br.rafaeros.fastrelax_api.features.collaborators.WorkDay;
import java.time.LocalTime;
import java.time.LocalDateTime;

public record CollaboratorWorkScheduleResponseDTO(
    Long id,
    Long collaboratorId,
    String collaboratorName,
    WorkDay dayOfWeek,
    /** Mesmo dia em português, pronto para exibição. */
    String dayOfWeekLabel,
    LocalTime allowedStartTime,
    LocalTime allowedEndTime,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime deletedAt
) {
    public CollaboratorWorkScheduleResponseDTO(CollaboratorWorkSchedule entity) {
        this(
            entity.getId(),
            entity.getCollaborator() != null ? entity.getCollaborator().getId() : null,
            entity.getCollaborator() != null ? entity.getCollaborator().getName() : null,
            entity.getDayOfWeek(),
            entity.getDayOfWeek() != null ? entity.getDayOfWeek().getLabel() : null,
            entity.getAllowedStartTime(),
            entity.getAllowedEndTime(),
            entity.isActive(),
            entity.getCreatedAt(),
            entity.getDeletedAt()
        );
    }
}
