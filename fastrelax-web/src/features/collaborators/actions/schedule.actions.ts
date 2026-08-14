"use server";

import { getWeeklySchedule } from "@/features/collaborators/services/schedule.service";
import type {
  AllowedWindow,
  CollaboratorSchedule,
} from "@/features/collaborators/types/schedule.types";

export type ScheduleResult =
  | { ok: true; windows: AllowedWindow[] }
  | { ok: false; message: string };

/**
 * Horário permitido do colaborador, já reduzido ao que a UI usa.
 * Dias inativos ficam de fora: são registros desativados, não janelas vigentes.
 */
export async function fetchCollaboratorScheduleAction(
  collaboratorId: number,
): Promise<ScheduleResult> {
  const result = await getWeeklySchedule(collaboratorId);

  if (!result.ok) {
    return { ok: false, message: result.message };
  }

  const windows = (result.data ?? [])
    .filter((schedule) => schedule.active)
    .map(toAllowedWindow)
    .filter((window): window is AllowedWindow => window !== null);

  return { ok: true, windows };
}

/**
 * Converte um dia da resposta em janela da UI.
 *
 * Aceita também os nomes antigos (`lunchStartTime`/`lunchEndTime`): durante o
 * deploy do rename, uma instância da API ainda no formato anterior deixaria a
 * tela vazia em vez de mostrar o horário que existe. Registro sem horário é
 * descartado — antes faltar uma linha do que quebrar a busca inteira.
 */
function toAllowedWindow(schedule: CollaboratorSchedule): AllowedWindow | null {
  const legacy = schedule as CollaboratorSchedule & {
    lunchStartTime?: string;
    lunchEndTime?: string;
  };

  const start = schedule.allowedStartTime ?? legacy.lunchStartTime;
  const end = schedule.allowedEndTime ?? legacy.lunchEndTime;

  if (!start || !end) return null;

  return {
    dayOfWeek: schedule.dayOfWeek,
    allowedStartTime: start.slice(0, 5),
    allowedEndTime: end.slice(0, 5),
  };
}
