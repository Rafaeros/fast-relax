"use server";

import { listSessions } from "@/features/sessions/services/session.service";
import { monthRange } from "@/features/sessions/lib/calendar";
import type {
  CalendarMonth,
  CollaboratorSession,
  SessionStatus,
} from "@/features/sessions/types/session.types";

export type MonthSessionsResult =
  | { ok: true; sessions: CollaboratorSession[]; truncated: boolean }
  | { ok: false; message: string };

/**
 * Sessões de todos os colaboradores no mês.
 *
 * O intervalo é montado no servidor a partir de ano/mês: uma data construída no
 * navegador pode cair no mês vizinho conforme o fuso da máquina.
 */
export async function fetchMonthSessions(
  month: CalendarMonth,
  status?: SessionStatus,
): Promise<MonthSessionsResult> {
  const { from, to } = monthRange(month);
  const result = await listSessions({ from, to, status });

  if (!result.ok) {
    return { ok: false, message: result.message };
  }

  return {
    ok: true,
    sessions: result.data.content ?? [],
    // Mês acima do teto da página: o aviso aparece em tela em vez de o
    // calendário mostrar menos sessões sem explicar por quê.
    truncated: !(result.data.last ?? true),
  };
}
