package br.rafaeros.fastrelax_api.features.collaborators.dtos;

import br.rafaeros.fastrelax_api.features.collaborators.CollaboratorSession;
import br.rafaeros.fastrelax_api.features.collaborators.SessionStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * @param status      valor do enum, estável para comparação no cliente
 * @param statusLabel mesmo status em português, pronto para exibição
 * @param chairId     cadeira que atendeu; nula enquanto a sessão não foi iniciada
 */
public record CollaboratorSessionResponseDTO(
    Long id,
    Long collaboratorId,
    String collaboratorName,
    LocalDate sessionDate,
    LocalTime startTime,
    LocalTime endTime,
    SessionStatus status,
    String statusLabel,
    Long chairId,
    String chairName,
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
            entity.getStatus() != null ? entity.getStatus().getLabel() : null,
            entity.getChair() != null ? entity.getChair().getId() : null,
            entity.getChair() != null ? entity.getChair().getName() : null,
            entity.getStartedAt(),
            entity.getFinishedAt(),
            entity.getCreatedAt()
        );
    }
}
