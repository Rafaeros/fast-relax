package br.rafaeros.fastrelax_api.features.collaborators.dtos;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import br.rafaeros.fastrelax_api.features.collaborators.WorkDay;

/**
 * Um dia da grade, com a janela de almoço do colaborador e todos os horários
 * dela — livres e ocupados, distinguidos pelo {@code available} de cada slot.
 */
public record AvailableDayDTO(
    LocalDate sessionDate,
    WorkDay dayOfWeek,
    LocalTime lunchStartTime,
    LocalTime lunchEndTime,
    List<SessionSlotDTO> slots
) {}
