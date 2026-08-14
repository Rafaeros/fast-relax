package br.rafaeros.fastrelax_api.features.collaborators.dtos;

import java.time.LocalDate;

import br.rafaeros.fastrelax_api.features.collaborators.SessionStatus;

/**
 * @param sessionDate dia exato
 * @param from        início do intervalo, inclusivo — o painel do RH monta o mês
 *                    com ele em vez de disparar uma consulta por dia
 * @param to          fim do intervalo, inclusivo
 */
public record CollaboratorSessionFilterDTO(
    SessionStatus status,
    Long collaboratorId,
    LocalDate sessionDate,
    LocalDate from,
    LocalDate to
) {}
