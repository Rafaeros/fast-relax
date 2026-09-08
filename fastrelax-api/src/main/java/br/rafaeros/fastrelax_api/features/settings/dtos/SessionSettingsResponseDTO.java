package br.rafaeros.fastrelax_api.features.settings.dtos;

import java.time.LocalDateTime;

import br.rafaeros.fastrelax_api.features.settings.CompanySessionSettings;
import br.rafaeros.fastrelax_api.features.settings.SessionQuotaPeriod;

/**
 * @param sessionQuotaPeriodLabel mesmo período em português, pronto para
 *                                exibição — o nome do enum continua sendo o
 *                                valor trafegado
 */
public record SessionSettingsResponseDTO(
    int defaultDurationMinutes,
    int startGraceMinutes,
    int maxAdvanceDays,
    int stabilizationMinutes,
    int sessionQuotaLimit,
    SessionQuotaPeriod sessionQuotaPeriod,
    String sessionQuotaPeriodLabel,
    LocalDateTime updatedAt
) {
    public SessionSettingsResponseDTO(CompanySessionSettings entity) {
        this(entity.getDefaultDurationMinutes(), entity.getStartGraceMinutes(), entity.getMaxAdvanceDays(),
                entity.getStabilizationMinutes(), entity.getSessionQuotaLimit(), entity.getSessionQuotaPeriod(),
                entity.getSessionQuotaPeriod() != null ? entity.getSessionQuotaPeriod().getLabel() : null,
                entity.getUpdatedAt());
    }
}
