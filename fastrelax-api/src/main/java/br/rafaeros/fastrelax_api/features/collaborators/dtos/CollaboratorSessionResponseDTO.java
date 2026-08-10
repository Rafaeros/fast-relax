package br.rafaeros.fastrelax_api.features.collaborators.dtos;

import br.rafaeros.fastrelax_api.features.collaborators.CollaboratorSession;
import br.rafaeros.fastrelax_api.features.collaborators.SessionStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

public record CollaboratorSessionResponseDTO(
    Long id,
    Long collaboratorId,
    String collaboratorName,
    LocalDate sessionDate,
    LocalTime startTime,
    LocalTime endTime,
    SessionStatus status,
    LocalDateTime startedAt,
    LocalDateTime finishedAt,
    LocalDateTime createdAt
) {
    public CollaboratorSessionResponseDTO(CollaboratorSession entity) {
        this(
            entity.getId(),
            entity.getCollaborator() != null ? entity.getCollaborator().getId() : null,
            entity.getCollaborator() != null ? entity.getCollaborator().getName() : null,
            entity.getSessionDate(),
            entity.getStartTime(),
            entity.getEndTime(),
            entity.getStatus(),
            entity.getStartedAt(),
            entity.getFinishedAt(),
            entity.getCreatedAt()
        );
    }
}
