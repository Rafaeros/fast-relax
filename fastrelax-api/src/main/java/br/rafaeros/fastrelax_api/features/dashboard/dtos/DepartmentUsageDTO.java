package br.rafaeros.fastrelax_api.features.dashboard.dtos;

/** Uso por departamento no período. */
public record DepartmentUsageDTO(
    Long departmentId,
    String departmentName,
    long totalSessions,
    long done,
    long expired
) {}
