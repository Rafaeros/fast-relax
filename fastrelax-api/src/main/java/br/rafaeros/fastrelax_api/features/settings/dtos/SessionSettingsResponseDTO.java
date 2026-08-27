package br.rafaeros.fastrelax_api.features.settings.dtos;

import java.time.LocalDateTime;

import br.rafaeros.fastrelax_api.features.settings.CompanySessionSettings;

public record SessionSettingsResponseDTO(
    int defaultDurationMinutes,
    int startGraceMinutes,
    int earlyStartMinutes,
    int maxAdvanceDays,
    LocalDateTime updatedAt
) {
    public SessionSettingsResponseDTO(CompanySessionSettings entity) {
        this(entity.getDefaultDurationMinutes(), entity.getStartGraceMinutes(),
                entity.getEarlyStartMinutes(), entity.getMaxAdvanceDays(), entity.getUpdatedAt());
    }
}
