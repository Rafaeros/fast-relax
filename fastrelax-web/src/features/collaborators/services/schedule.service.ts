import { apiFetch } from "@/lib/api/http";
import type { ApiResult } from "@/lib/api/api.types";
import { readAccessToken } from "@/features/authentication/services/session.service";
import type {
  AllowedWindow,
  CollaboratorSchedule,
} from "@/features/collaborators/types/schedule.types";

/**
 * Horário permitido do colaborador (`/collaborators/{id}/schedules`).
 * É a janela em que ele pode agendar sessão — um registro por dia da semana.
 */

export async function getWeeklySchedule(
  collaboratorId: number,
): Promise<ApiResult<CollaboratorSchedule[]>> {
  return apiFetch<CollaboratorSchedule[]>(`/collaborators/${collaboratorId}/schedules`, {
    token: await readAccessToken(),
  });
}

/**
 * Substitui a semana inteira: dias fora da lista são desativados.
 * O backend recusa lista vazia (`@NotEmpty`), então quem chama só envia quando
 * há ao menos um dia configurado.
 */
export async function replaceWeeklySchedule(
  collaboratorId: number,
  schedules: AllowedWindow[],
): Promise<ApiResult<CollaboratorSchedule[]>> {
  return apiFetch<CollaboratorSchedule[]>(`/collaborators/${collaboratorId}/schedules`, {
    method: "PUT",
    body: { schedules },
    token: await readAccessToken(),
  });
}
