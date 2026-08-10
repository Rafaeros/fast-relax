package br.rafaeros.fastrelax_api.features.dashboard.dtos;

import java.time.LocalDate;
import java.util.List;

/**
 * Números do período para o painel do RH, calculados no banco em vez de o
 * cliente paginar tudo e contar.
 *
 * @param attendanceRate percentual de sessões concluídas sobre as encerradas
 *                       (concluídas + expiradas + canceladas); null quando não
 *                       houve nenhuma encerrada no período
 */
public record DashboardResponseDTO(
    LocalDate from,
    LocalDate to,
    long totalSessions,
    long scheduled,
    long inProgress,
    long done,
    long expired,
    long cancelled,
    Double attendanceRate,
    long activeCollaborators,
    long collaboratorsWithoutSchedule,
    List<DepartmentUsageDTO> byDepartment,
    List<DailyUsageDTO> byDay
) {}
