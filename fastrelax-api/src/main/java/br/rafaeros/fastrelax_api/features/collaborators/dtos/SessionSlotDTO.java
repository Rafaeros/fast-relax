package br.rafaeros.fastrelax_api.features.collaborators.dtos;

import java.time.LocalTime;

/**
 * Horário dentro da janela de horário permitido do colaborador.
 *
 * @param available false quando já existe sessão ativa no intervalo ou quando o
 *                  horário do dia de hoje já passou. Ocupados continuam na lista
 *                  para que a tela possa exibi-los desabilitados.
 */
public record SessionSlotDTO(
    LocalTime startTime,
    LocalTime endTime,
    boolean available
) {}
