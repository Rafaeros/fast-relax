package br.rafaeros.fastrelax_api.features.collaborators.dtos;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * Whole-week payload for {@code PUT /collaborators/{id}/schedules}.
 *
 * <p>
 * Days omitted from the list are deactivated, so this defines the collaborator's
 * complete schedule rather than adding to it. A subset is allowed — part-time and
 * shift collaborators do not have lunch configured every weekday.
 */
public record WeeklyScheduleRequestDTO(
    @NotEmpty(message = "Informe ao menos um dia da semana")
    @Size(max = 6, message = "A semana tem no máximo 6 dias úteis (segunda a sábado)")
    @Valid
    List<WorkScheduleItemDTO> schedules
) {}
