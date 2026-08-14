"use client";

import { Card, CardTitle } from "@/components/ui";
import { cn } from "@/lib/cn";
import type { DepartmentUsage } from "@/features/dashboard/types/dashboard.types";

/** Quantos departamentos aparecem antes de o resto virar uma linha só. */
const TOP_LIMIT = 8;

export type DepartmentUsageChartProps = {
  data: DepartmentUsage[];
  className?: string;
};

/**
 * Uso por departamento: barras horizontais, série única.
 *
 * Magnitude comparada entre categorias — uma matiz só, sem legenda: o rótulo de
 * cada linha já identifica a barra.
 */
export function DepartmentUsageChart({ data, className }: DepartmentUsageChartProps) {
  const sorted = [...data].sort((a, b) => b.totalSessions - a.totalSessions);
  const top = sorted.slice(0, TOP_LIMIT);
  const rest = sorted.slice(TOP_LIMIT);

  // O excedente é somado numa linha, nunca descartado em silêncio.
  const rows = [
    ...top.map((entry) => ({
      key: String(entry.departmentId ?? entry.departmentName ?? "sem-departamento"),
      name: entry.departmentName ?? "Sem departamento",
      total: entry.totalSessions,
      done: entry.done,
    })),
    ...(rest.length > 0
      ? [
          {
            key: "outros",
            name: `Outros ${rest.length} departamentos`,
            total: rest.reduce((sum, entry) => sum + entry.totalSessions, 0),
            done: rest.reduce((sum, entry) => sum + entry.done, 0),
          },
        ]
      : []),
  ];

  const max = Math.max(1, ...rows.map((row) => row.total));

  return (
    <Card padding="lg" className={cn("flex flex-col gap-6", className)}>
      <CardTitle>Sessões por departamento</CardTitle>

      {rows.length === 0 ? (
        <p className="py-12 text-center text-sm text-ink-tertiary">
          Nenhuma sessão registrada no período.
        </p>
      ) : (
        <ul className="flex flex-col gap-4">
          {rows.map((row) => (
            <li key={row.key} className="flex flex-col gap-1.5">
              <div className="flex items-baseline justify-between gap-3">
                <span className="truncate text-sm text-ink-secondary">{row.name}</span>
                <span className="text-sm font-semibold tabular-nums text-ink-primary">
                  {row.total}
                </span>
              </div>

              <div
                className="h-2 w-full overflow-hidden rounded-[4px] bg-bg-900"
                role="img"
                aria-label={`${row.name}: ${row.total} sessões, ${row.done} concluídas`}
              >
                <div
                  className="h-full rounded-[4px] bg-accent"
                  style={{ width: `${(row.total / max) * 100}%` }}
                />
              </div>

              <span className="text-[0.6875rem] text-ink-tertiary">
                {row.done} concluída{row.done === 1 ? "" : "s"}
              </span>
            </li>
          ))}
        </ul>
      )}
    </Card>
  );
}
