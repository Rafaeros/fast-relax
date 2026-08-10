package br.rafaeros.fastrelax_api.features.collaborators.dtos;

import java.time.LocalTime;

import br.rafaeros.fastrelax_api.features.collaborators.WorkDay;
import jakarta.validation.constraints.NotNull;

/** Single-day schedule, for adjusting one weekday without touching the rest of the week. */
public record CollaboratorWorkScheduleDTO(
    @NotNull(message = "O colaborador é obrigatório")
    Long collaboratorId,

    @NotNull(message = "O dia da semana é obrigatório")
    WorkDay dayOfWeek,

    @NotNull(message = "O horário de início do almoço é obrigatório")
    LocalTime lunchStartTime,

    @NotNull(message = "O horário de término do almoço é obrigatório")
    LocalTime lunchEndTime,

    boolean active
) {}
