package br.rafaeros.fastrelax_api.features.notifications.dtos;

import java.time.LocalDateTime;

import br.rafaeros.fastrelax_api.features.notifications.DeviceToken;

public record DeviceTokenResponseDTO(
    Long id,
    Long collaboratorId,
    DeviceToken.Platform platform,
    boolean active,
    LocalDateTime createdAt
) {
    public DeviceTokenResponseDTO(DeviceToken entity) {
        this(
            entity.getId(),
            entity.getCollaborator() != null ? entity.getCollaborator().getId() : null,
            entity.getPlatform(),
            entity.isActive(),
            entity.getCreatedAt()
        );
    }
}
