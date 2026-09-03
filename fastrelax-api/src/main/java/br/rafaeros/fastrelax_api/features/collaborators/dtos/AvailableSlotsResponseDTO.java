package br.rafaeros.fastrelax_api.features.collaborators.dtos;

import java.time.LocalDate;
import java.util.List;

/**
 * Grade de horários livres de um colaborador dentro de um período.
 *
 * <p>
 * Só entram os dias em que o colaborador tem janela de horário permitido — domingo e dias
 * não cadastrados ficam de fora. Dias lotados aparecem normalmente, com todos os
 * slots marcados como indisponíveis, para a tela poder exibi-los desabilitados.
 */
public record AvailableSlotsResponseDTO(
    LocalDate from,
    LocalDate to,
    int durationMinutes,
    int stabilizationMinutes,
    int maxAdvanceDays,
    List<AvailableDayDTO> days
) {}
