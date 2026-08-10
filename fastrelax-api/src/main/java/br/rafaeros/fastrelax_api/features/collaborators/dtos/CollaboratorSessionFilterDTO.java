package br.rafaeros.fastrelax_api.features.collaborators.dtos;

import java.time.LocalDate;

import br.rafaeros.fastrelax_api.features.collaborators.SessionStatus;

public record CollaboratorSessionFilterDTO(
    SessionStatus status,
    Long collaboratorId,
    LocalDate sessionDate
) {}
