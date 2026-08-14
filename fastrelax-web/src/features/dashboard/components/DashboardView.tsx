"use client";

import { Alert, Icon, buttonStyles } from "@/components/ui";
import { cn } from "@/lib/cn";
import { formatLongDate } from "@/lib/format";
import { useDashboardSummary } from "@/features/dashboard/hooks/useDashboardSummary";
import { DailySessionsChart } from "@/features/dashboard/components/DailySessionsChart";
import { DashboardMetrics } from "@/features/dashboard/components/DashboardMetrics";
import { DepartmentUsageChart } from "@/features/dashboard/components/DepartmentUsageChart";
import { SessionStatusBreakdown } from "@/features/dashboard/components/SessionStatusBreakdown";
import {
  DASHBOARD_PERIODS,
  PERIOD_LABELS,
  type DashboardSummary,
} from "@/features/dashboard/types/dashboard.types";

export type DashboardViewProps = {
  initialSummary: DashboardSummary;
  /** Mensagem da API quando a primeira carga falhou no servidor. */
  initialError?: string;
};

export function DashboardView({ initialSummary, initialError }: DashboardViewProps) {
  const { summary, period, pending, error, changePeriod } = useDashboardSummary(initialSummary);
  const message = error ?? initialError;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className="text-sm text-ink-secondary">
          {summary.from && summary.to
            ? `De ${formatLongDate(summary.from)} a ${formatLongDate(summary.to)}`
            : "Período não carregado"}
        </p>

        {/* Filtro numa linha só, acima dos gráficos. */}
        <div className="flex items-center gap-2" role="group" aria-label="Período">
          {pending && <Icon name="loader" className="h-4 w-4 animate-spin text-ink-tertiary" />}
          {DASHBOARD_PERIODS.map((option) => (
            <button
              key={option}
              type="button"
              onClick={() => changePeriod(option)}
              aria-pressed={period === option}
              disabled={pending}
              className={cn(
                buttonStyles({ variant: period === option ? "primary" : "secondary", size: "sm" }),
              )}
            >
              {PERIOD_LABELS[option]}
            </button>
          ))}
        </div>
      </div>

      {message && (
        <Alert tone="error" title="Não foi possível carregar as métricas">
          {message}
        </Alert>
      )}

      <DashboardMetrics summary={summary} />

      <DailySessionsChart data={summary.byDay} />

      <div className="grid gap-4 lg:grid-cols-2">
        <DepartmentUsageChart data={summary.byDepartment} />
        <SessionStatusBreakdown summary={summary} />
      </div>
    </div>
  );
}
