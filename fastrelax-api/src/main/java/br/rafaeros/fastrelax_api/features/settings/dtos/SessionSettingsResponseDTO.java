package br.rafaeros.fastrelax_api.features.settings.dtos;

import java.time.LocalDateTime;

import br.rafaeros.fastrelax_api.features.settings.SessionSettings;

public record SessionSettingsResponseDTO(
    int defaultDurationMinutes,
    int startGraceMinutes,
    int maxAdvanceDays,
    LocalDateTime updatedAt
) {
    public SessionSettingsResponseDTO(SessionSettings entity) {
        this(entity.getDefaultDurationMinutes(), entity.getStartGraceMinutes(), entity.getMaxAdvanceDays(),
                entity.getUpdatedAt());
    }
}
