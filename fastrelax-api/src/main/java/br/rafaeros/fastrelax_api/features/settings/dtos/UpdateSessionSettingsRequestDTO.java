package br.rafaeros.fastrelax_api.features.settings.dtos;

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

    /** Quantos minutos antes do horário agendado a sessão já pode ser iniciada. */
    @NotNull(message = "A antecedência de início é obrigatória")
    @Min(value = 0, message = "A antecedência deve ser de no mínimo 0 minutos")
    @Max(value = 60, message = "A antecedência deve ser de no máximo 60 minutos")
    Integer earlyStartMinutes,

    /** Quantos dias à frente é possível agendar. */
    @NotNull(message = "A antecedência máxima é obrigatória")
    @Min(value = 1, message = "A antecedência deve ser de no mínimo 1 dia")
    @Max(value = 365, message = "A antecedência deve ser de no máximo 365 dias")
    Integer maxAdvanceDays
) {}
