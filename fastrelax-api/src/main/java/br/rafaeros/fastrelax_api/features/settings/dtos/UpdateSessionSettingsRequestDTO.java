package br.rafaeros.fastrelax_api.features.settings.dtos;

import br.rafaeros.fastrelax_api.features.settings.SessionQuotaPeriod;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateSessionSettingsRequestDTO(
    // Os limites espelham o CHECK da tabela, para o erro sair como 400 e não como
    // violação de constraint.
    @NotNull(message = "A duração padrão é obrigatória")
    @Min(value = 1, message = "A duração deve ser de no mínimo 1 minuto")
    @Max(value = 120, message = "A duração deve ser de no máximo 120 minutos")
    Integer defaultDurationMinutes,

    /** Zero expira a sessão assim que o horário de início passa. */
    @NotNull(message = "A tolerância de início é obrigatória")
    @Min(value = 0, message = "A tolerância deve ser de no mínimo 0 minutos")
    @Max(value = 60, message = "A tolerância deve ser de no máximo 60 minutos")
    Integer startGraceMinutes,

    /** Quantos dias à frente é possível agendar. */
    @NotNull(message = "A antecedência máxima é obrigatória")
    @Min(value = 1, message = "A antecedência deve ser de no mínimo 1 dia")
    @Max(value = 365, message = "A antecedência deve ser de no máximo 365 dias")
    Integer maxAdvanceDays,

    /** Minutos mínimos entre o fim de uma sessão e o início da próxima na mesma cadeira. */
    @NotNull(message = "A estabilização é obrigatória")
    @Min(value = 0, message = "A estabilização deve ser de no mínimo 0 minutos")
    @Max(value = 30, message = "A estabilização deve ser de no máximo 30 minutos")
    Integer stabilizationMinutes,

    /** Quantas massagens cada colaborador pode ter dentro do período da cota. */
    @NotNull(message = "O limite de massagens é obrigatório")
    @Min(value = 1, message = "O limite deve ser de no mínimo 1 massagem")
    @Max(value = 20, message = "O limite deve ser de no máximo 20 massagens")
    Integer sessionQuotaLimit,

    /**
     * Janela em que o limite conta. {@code ACTIVE} não olha calendário: são as
     * massagens marcadas ou em andamento ao mesmo tempo.
     */
    @NotNull(message = "O período da cota é obrigatório")
    SessionQuotaPeriod sessionQuotaPeriod
) {}
