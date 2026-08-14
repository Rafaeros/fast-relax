"use client";

import { Card, CardTitle, Icon } from "@/components/ui";
import type { IconName } from "@/components/ui";
import { cn } from "@/lib/cn";
import type { DashboardSummary } from "@/features/dashboard/types/dashboard.types";

export type SessionStatusBreakdownProps = {
  summary: DashboardSummary;
  className?: string;
};

/**
 * Distribuição por status do ciclo de vida da sessão.
 * Cor de status nunca aparece sozinha: cada linha traz ícone e rótulo.
 */
export function SessionStatusBreakdown({ summary, className }: SessionStatusBreakdownProps) {
  const statuses: { label: string; value: number; icon: IconName; color: string }[] = [
    { label: "Concluídas", value: summary.done, icon: "check", color: "var(--color-success-400)" },
    { label: "Agendadas", value: summary.scheduled, icon: "calendar", color: "var(--color-info-500)" },
    { label: "Em andamento", value: summary.inProgress, icon: "play", color: "var(--color-copper-400)" },
    { label: "Expiradas", value: summary.expired, icon: "alert", color: "var(--color-error-600)" },
    { label: "Canceladas", value: summary.cancelled, icon: "close", color: "var(--color-neutral-400)" },
  ];

  const total = Math.max(1, summary.totalSessions);

  return (
    <Card padding="lg" className={cn("flex flex-col gap-5", className)}>
      <CardTitle>Situação das sessões</CardTitle>

      <ul className="flex flex-col gap-4">
        {statuses.map((status) => (
          <li key={status.label} className="flex flex-col gap-1.5">
            <div className="flex items-center gap-2.5">
              <Icon
                name={status.icon}
                className="h-4 w-4 shrink-0"
                style={{ color: status.color }}
              />
              <span className="flex-1 text-sm text-ink-secondary">{status.label}</span>
              <span className="text-sm font-semibold tabular-nums text-ink-primary">
                {status.value}
              </span>
              <span className="w-12 text-right text-xs tabular-nums text-ink-tertiary">
                {Math.round((status.value / total) * 100)}%
              </span>
            </div>

            <div className="h-1.5 w-full overflow-hidden rounded-[4px] bg-bg-900">
              <div
                className="h-full rounded-[4px]"
                style={{
                  width: `${(status.value / total) * 100}%`,
                  backgroundColor: status.color,
                }}
              />
            </div>
          </li>
        ))}
      </ul>
    </Card>
  );
}
