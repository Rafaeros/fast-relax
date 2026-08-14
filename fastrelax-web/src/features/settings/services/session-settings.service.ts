import { apiFetch } from "@/lib/api/http";
import type { ApiResult } from "@/lib/api/api.types";
import { readAccessToken } from "@/features/authentication/services/session.service";
import type {
  SessionSettings,
  UpdateSessionSettingsInput,
} from "@/features/settings/types/session-settings.types";

/**
 * Configurações globais das sessões (`/settings/sessions`).
 * Leitura é liberada a qualquer autenticado; a alteração exige ADMIN ou RH.
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
