"use client";

import { useState, useTransition } from "react";
import { fetchDashboardSummary } from "@/features/dashboard/actions/dashboard.actions";
import {
  DEFAULT_DASHBOARD_PERIOD,
  type DashboardPeriod,
  type DashboardSummary,
} from "@/features/dashboard/types/dashboard.types";

export type UseDashboardSummaryReturn = {
  summary: DashboardSummary;
  period: DashboardPeriod;
  pending: boolean;
  error: string | null;
  changePeriod: (period: DashboardPeriod) => void;
};

/**
 * Métricas do painel com troca de período.
 * A primeira carga vem do servidor; as trocas chamam a Server Action, então o
 * token continua fora do navegador.
 */
export function useDashboardSummary(initial: DashboardSummary): UseDashboardSummaryReturn {
  const [summary, setSummary] = useState(initial);
  const [period, setPeriod] = useState<DashboardPeriod>(DEFAULT_DASHBOARD_PERIOD);
  const [error, setError] = useState<string | null>(null);
  const [pending, startTransition] = useTransition();

  const changePeriod = (next: DashboardPeriod) => {
    setPeriod(next);

    startTransition(async () => {
      const result = await fetchDashboardSummary(next);

      if (!result.ok) {
        setError(result.message);
        return;
      }

      setError(null);
      setSummary(result.summary);
    });
  };

  return { summary, period, pending, error, changePeriod };
}
