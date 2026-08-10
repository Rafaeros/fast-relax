package br.rafaeros.fastrelax_api.features.collaborators.dtos;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

/**
 * Agendamento de sessão.
 *
 * <p>
 * Sem {@code status}: a sessão nasce {@code SCHEDULED} e muda de estado pelos
 * endpoints de transição, nunca por atribuição direta do cliente.
 *
 * <p>
 * Sem {@code endTime}: é calculado como {@code startTime} mais a duração padrão
 * configurada pelo RH, para que nenhum agendamento fuja do tempo definido.
 */
public record CollaboratorSessionDTO(
    @NotNull(message = "O colaborador é obrigatório")
    Long collaboratorId,

    @NotNull(message = "A data da sessão é obrigatória")
    LocalDate sessionDate,

    @NotNull(message = "O horário de início é obrigatório")
    LocalTime startTime
) {}
