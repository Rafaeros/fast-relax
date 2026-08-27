import { apiFetch } from "@/lib/api/http";
import type { ApiResult } from "@/lib/api/api.types";
import { readAccessToken } from "@/features/authentication/services/session.service";
import type {
  SessionSettings,
  UpdateSessionSettingsInput,
} from "@/features/settings/types/session-settings.types";

/**
 * Configurações de sessão da empresa (`/settings/sessions`).
 *
 * Deixaram de ser globais quando o produto virou multi-empresa: duração e
 * antecedência são acordos de cada contrato. O backend resolve a empresa pelo
 * token, então não há id na URL.
 *
 * Leitura é liberada a qualquer autenticado; alterar exige gestor ou RH.
 */

const RESOURCE = "/settings/sessions";

export async function getSessionSettings(): Promise<ApiResult<SessionSettings>> {
  return apiFetch<SessionSettings>(RESOURCE, { token: await readAccessToken() });
}

export async function updateSessionSettings(
  input: UpdateSessionSettingsInput,
): Promise<ApiResult<SessionSettings>> {
  return apiFetch<SessionSettings>(RESOURCE, {
    method: "PUT",
    body: input,
    token: await readAccessToken(),
  });
}
