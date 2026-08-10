package br.rafaeros.fastrelax_api.features.dashboard.dtos;

import java.time.LocalDate;

/** Série diária para o gráfico do painel. */
public record DailyUsageDTO(
    LocalDate sessionDate,
    long totalSessions,
    long done,
    long expired
) {}
