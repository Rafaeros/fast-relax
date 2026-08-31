package br.rafaeros.fastrelax_api.features.collaborators.dtos;

import java.time.LocalTime;
import java.util.List;

/**
 * Horário dentro da janela de horário permitido do colaborador.
 *
 * @param availableChairs lista de cadeiras disponíveis neste horário. Vazia
 *                        quando já existe sessão ativa em todas as cadeiras ou
 *                        quando o horário de hoje já passou. Ocupados continuam
 *                        na lista para que a tela possa exibi-los desabilitados.
 */
public record SessionSlotDTO(
    LocalTime startTime,
    LocalTime endTime,
    List<AvailableChairDTO> availableChairs
) {}
