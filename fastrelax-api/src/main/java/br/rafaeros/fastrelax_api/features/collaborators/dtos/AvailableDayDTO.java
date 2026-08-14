package br.rafaeros.fastrelax_api.features.collaborators.dtos;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import br.rafaeros.fastrelax_api.features.collaborators.WorkDay;

/**
 * Um dia da grade, com a janela de horário permitido do colaborador e todos os horários
 * dela — livres e ocupados, distinguidos pelo {@code available} de cada slot.
 */
public record AvailableDayDTO(
    LocalDate sessionDate,
    WorkDay dayOfWeek,
    /** Mesmo dia em português, pronto para exibição. */
    String dayOfWeekLabel,
    LocalTime allowedStartTime,
    LocalTime allowedEndTime,
    List<SessionSlotDTO> slots
) {
    public AvailableDayDTO(LocalDate sessionDate, WorkDay dayOfWeek, LocalTime allowedStartTime,
            LocalTime allowedEndTime, List<SessionSlotDTO> slots) {
        this(sessionDate, dayOfWeek, dayOfWeek != null ? dayOfWeek.getLabel() : null,
                allowedStartTime, allowedEndTime, slots);
    }
}
