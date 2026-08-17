"use server";

import { revalidatePath } from "next/cache";
import { updateSessionSettings } from "@/features/settings/services/session-settings.service";
import {
  mapSessionSettingsApiErrors,
  validateSessionSettingsInput,
} from "@/features/settings/schemas/session-settings.schema";
import type { SessionSettingsFormState } from "@/features/settings/types/session-settings.types";

const ROUTE = "/painel/configuracao-sessao";

/**
 * Salva as configurações globais das sessões.
 *
 * O `PUT` substitui os três valores de uma vez — é assim que o endpoint
 * funciona, então o formulário sempre envia o conjunto completo, mesmo quando
 * só um campo mudou.
 */
export async function updateSessionSettingsAction(
  _previousState: SessionSettingsFormState,
  formData: FormData,
): Promise<SessionSettingsFormState> {
  const validation = validateSessionSettingsInput({
    defaultDurationMinutes: String(formData.get("defaultDurationMinutes") ?? ""),
    startGraceMinutes: String(formData.get("startGraceMinutes") ?? ""),
    earlyStartMinutes: String(formData.get("earlyStartMinutes") ?? ""),
    maxAdvanceDays: String(formData.get("maxAdvanceDays") ?? ""),
  });

  if (!validation.valid) {
    return {
      status: "error",
      message: "Confira os campos destacados.",
      fieldErrors: validation.fieldErrors,
    };
  }

  const result = await updateSessionSettings(validation.data);

  if (!result.ok) {
    return {
      status: "error",
      message: result.message,
      fieldErrors: mapSessionSettingsApiErrors(result.errors),
    };
  }

  // A duração também alimenta os horários exibidos no app do colaborador.
  revalidatePath(ROUTE);
  return { status: "success", message: result.message, settings: result.data };
}
