"use server";

import { getDashboardSummary } from "@/features/dashboard/services/dashboard.service";
import type { DashboardSummary } from "@/features/dashboard/types/dashboard.types";

export type DashboardResult =
  | { ok: true; summary: DashboardSummary }
  | { ok: false; message: string };

/**
 * Métricas dos últimos `days` dias.
 *
 * O intervalo é calculado aqui e não no navegador: o backend trabalha no fuso
 * da aplicação (America/Sao_Paulo), e uma data montada no cliente pode cair no
 * dia errado dependendo do fuso da máquina.
 */
export async function fetchDashboardSummary(days: number): Promise<DashboardResult> {
  const to = new Date();
  const from = new Date();
  from.setDate(to.getDate() - days);

  const result = await getDashboardSummary({
    from: toIsoDate(from),
    to: toIsoDate(to),
  });

  if (!result.ok) {
    return { ok: false, message: result.message };
  }

  return { ok: true, summary: result.data };
}

function toIsoDate(date: Date): string {
  return date.toISOString().slice(0, 10);
}
