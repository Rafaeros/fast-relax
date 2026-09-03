package br.rafaeros.fastrelax_api.features.settings.dtos;

import java.time.LocalDateTime;

import br.rafaeros.fastrelax_api.features.settings.CompanySessionSettings;

public record SessionSettingsResponseDTO(
    int defaultDurationMinutes,
    int startGraceMinutes,
    int maxAdvanceDays,
    int stabilizationMinutes,
    LocalDateTime updatedAt
) {
    public SessionSettingsResponseDTO(CompanySessionSettings entity) {
        this(entity.getDefaultDurationMinutes(), entity.getStartGraceMinutes(), entity.getMaxAdvanceDays(),
                entity.getStabilizationMinutes(), entity.getUpdatedAt());
    }
}
