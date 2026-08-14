"use client";

import { Card, CardTitle } from "@/components/ui";
import { cn } from "@/lib/cn";
import type { DailyUsage } from "@/features/dashboard/types/dashboard.types";

/**
 * Paleta das séries.
 *
 * Verde/vermelho puro reprova em deuteranopia (ΔE 5,3): estes três passam nos
 * testes de CVD, separação em visão normal e contraste sobre a superfície do
 * card. O verde é propositalmente mais claro — é a diferença de luminosidade
 * que garante a separação de quem não distingue as matizes.
 */
const SERIES = [
  { key: "done", label: "Concluídas", color: "var(--color-success-400)" },
  { key: "expired", label: "Expiradas", color: "var(--color-error-600)" },
  { key: "other", label: "Outras", color: "var(--color-info-500)" },
] as const;

type Segment = { key: string; label: string; color: string; value: number };

type Day = {
  date: string;
  label: string;
  total: number;
  segments: Segment[];
};

const dayFormatter = new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit" });

function toDay(entry: DailyUsage): Day {
  // "Outras" é o resto: agendadas, em andamento e canceladas do dia, que a API
  // não quebra por status na série diária.
  const other = Math.max(0, entry.totalSessions - entry.done - entry.expired);
  const [year, month, day] = entry.sessionDate.split("-").map(Number);

  return {
    date: entry.sessionDate,
    label: dayFormatter.format(new Date(year, (month ?? 1) - 1, day ?? 1)),
    total: entry.totalSessions,
    segments: [
      { ...SERIES[0], value: entry.done },
      { ...SERIES[1], value: entry.expired },
      { ...SERIES[2], value: other },
    ],
  };
}

export type DailySessionsChartProps = {
  data: DailyUsage[];
  className?: string;
};

export function DailySessionsChart({ data, className }: DailySessionsChartProps) {
  const days = data.map(toDay);
  const max = Math.max(1, ...days.map((day) => day.total));
  const hasData = days.some((day) => day.total > 0);
  // Rótulos a cada N dias: com 90 dias, um por coluna vira borrão.
  const labelEvery = Math.max(1, Math.ceil(days.length / 10));

  return (
    <Card padding="lg" className={cn("flex flex-col gap-6", className)}>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <CardTitle>Sessões por dia</CardTitle>

        <ul className="flex flex-wrap items-center gap-4">
          {SERIES.map((series) => (
            <li key={series.key} className="flex items-center gap-2 text-xs text-ink-secondary">
              <span
                aria-hidden="true"
                className="h-2.5 w-2.5 rounded-[2px]"
                style={{ backgroundColor: series.color }}
              />
              {series.label}
            </li>
          ))}
        </ul>
      </div>

      {!hasData ? (
        <p className="py-12 text-center text-sm text-ink-tertiary">
          Nenhuma sessão registrada no período.
        </p>
      ) : (
        <>
          <div className="flex h-48 items-end gap-1 overflow-x-auto">
            {days.map((day) => (
              <div
                key={day.date}
                tabIndex={0}
                className="group relative flex h-full min-w-2 flex-1 flex-col justify-end rounded-[4px] focus-visible:outline-none focus-visible:shadow-focus"
                aria-label={`${day.label}: ${day.total} sessões`}
              >
                {/* Empilhado de baixo para cima, com 2px de respiro entre faixas. */}
                <div
                  className="flex flex-col-reverse justify-start gap-[2px] overflow-hidden rounded-[4px]"
                  style={{ height: `${(day.total / max) * 100}%` }}
                >
                  {day.segments
                    .filter((segment) => segment.value > 0)
                    .map((segment) => (
                      <div
                        key={segment.key}
                        style={{
                          backgroundColor: segment.color,
                          height: `${(segment.value / Math.max(1, day.total)) * 100}%`,
                        }}
                      />
                    ))}
                </div>

                <div
                  role="tooltip"
                  className={cn(
                    "pointer-events-none absolute bottom-full left-1/2 z-10 mb-2 hidden -translate-x-1/2",
                    "min-w-36 rounded-control border border-line bg-surface-nav p-3 shadow-card-hover",
                    "group-hover:block group-focus-visible:block",
                  )}
                >
                  <span className="text-xs font-semibold text-ink-primary">{day.label}</span>
                  <ul className="mt-2 flex flex-col gap-1">
                    {day.segments.map((segment) => (
                      <li
                        key={segment.key}
                        className="flex items-center gap-2 text-xs whitespace-nowrap text-ink-secondary"
                      >
                        <span
                          aria-hidden="true"
                          className="h-2 w-2 rounded-[2px]"
                          style={{ backgroundColor: segment.color }}
                        />
                        {segment.label}
                        <span className="ml-auto font-semibold text-ink-primary">
                          {segment.value}
                        </span>
                      </li>
                    ))}
                  </ul>
                </div>
              </div>
            ))}
          </div>

          <div className="flex gap-1 text-[0.6875rem] text-ink-tertiary">
            {days.map((day, index) => (
              <span key={day.date} className="min-w-2 flex-1 text-center">
                {index % labelEvery === 0 ? day.label : ""}
              </span>
            ))}
          </div>

          {/* Alternativa não visual à leitura do gráfico. */}
          <details className="text-sm">
            <summary className="cursor-pointer text-xs text-ink-tertiary hover:text-ink-secondary">
              Ver dados em tabela
            </summary>
            <div className="mt-3 max-h-64 overflow-auto">
              <table className="w-full text-left text-xs">
                <thead className="sticky top-0 bg-surface-card text-ink-muted">
                  <tr>
                    <th className="py-2 font-semibold">Dia</th>
                    {SERIES.map((series) => (
                      <th key={series.key} className="py-2 text-right font-semibold">
                        {series.label}
                      </th>
                    ))}
                    <th className="py-2 text-right font-semibold">Total</th>
                  </tr>
                </thead>
                <tbody>
                  {days.map((day) => (
                    <tr key={day.date} className="border-t border-line-soft/60 text-ink-secondary">
                      <td className="py-1.5">{day.label}</td>
                      {day.segments.map((segment) => (
                        <td key={segment.key} className="py-1.5 text-right tabular-nums">
                          {segment.value}
                        </td>
                      ))}
                      <td className="py-1.5 text-right font-semibold tabular-nums text-ink-primary">
                        {day.total}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </details>
        </>
      )}
    </Card>
  );
}
