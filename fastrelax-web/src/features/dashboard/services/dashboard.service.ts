import { apiFetch } from "@/lib/api/http";
import type { ApiResult } from "@/lib/api/api.types";
import { buildQuery } from "@/lib/api/query";
import { readAccessToken } from "@/features/authentication/services/session.service";
import type { DashboardSummary } from "@/features/dashboard/types/dashboard.types";

/**
 * Métricas agregadas do painel (`GET /dashboard/sessions`), restritas a ADMIN e RH.
 * As contas são feitas no banco: o painel não pagina sessões para somar no cliente.
 */
export async function getDashboardSummary(params: {
  from?: string;
  to?: string;
} = {}): Promise<ApiResult<DashboardSummary>> {
  // Sem datas o backend assume os últimos 30 dias até hoje.
  const query = buildQuery({ from: params.from, to: params.to });

  return apiFetch<DashboardSummary>(`/dashboard/sessions${query}`, {
    token: await readAccessToken(),
  });
}
