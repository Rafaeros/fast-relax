package br.rafaeros.fastrelax_api.features.collaborators.dtos;

import br.rafaeros.fastrelax_api.features.collaborators.WorkDay;

public record CollaboratorWorkScheduleFilterDTO(
    WorkDay dayOfWeek,
    Boolean active,
    Long collaboratorId
) {}
