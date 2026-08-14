import type { PageParams } from "@/lib/api/pagination.types";

/** Espelha o enum `SessionStatus` do backend. */
export const SESSION_STATUSES = [
  "SCHEDULED",
  "STARTED",
  "DONE",
  "EXPIRED",
  "CANCELLED",
] as const;

export type SessionStatus = (typeof SESSION_STATUSES)[number];

export const SESSION_STATUS_LABELS: Record<SessionStatus, string> = {
  SCHEDULED: "Agendada",
  STARTED: "Em andamento",
  DONE: "Concluída",
  EXPIRED: "Expirada",
  CANCELLED: "Cancelada",
};

/** Tom do `Badge` por status — mesma leitura em tabela e calendário. */
export const SESSION_STATUS_TONES: Record<
  SessionStatus,
  "success" | "info" | "warning" | "error" | "neutral"
> = {
  SCHEDULED: "info",
  STARTED: "warning",
  DONE: "success",
  EXPIRED: "error",
  CANCELLED: "neutral",
};

/** Espelha `CollaboratorSessionResponseDTO`. */
export type CollaboratorSession = {
  id: number;
  collaboratorId: number | null;
  collaboratorName: string | null;
  /** "YYYY-MM-DD". */
  sessionDate: string;
  /** "HH:mm:ss". */
  startTime: string;
  endTime: string;
  status: SessionStatus;
  /** Mesmo status em português, enviado pela API para exibição. */
  statusLabel?: string;
  startedAt: string | null;
  finishedAt: string | null;
  createdAt: string;
};

/**
 * Espelha `CollaboratorSessionFilterDTO`.
 * `from`/`to` formam o intervalo do mês exibido na agenda.
 */
export type SessionFilter = {
  status?: SessionStatus;
  collaboratorId?: number;
  sessionDate?: string;
  from?: string;
  to?: string;
};

export type ListSessionsParams = SessionFilter & PageParams;

/** Mês exibido na agenda. `month` é 1-12, como o usuário lê. */
export type CalendarMonth = {
  year: number;
  month: number;
};

/** "HH:mm:ss" → "HH:mm"; o segundo nunca importa na leitura da agenda. */
export function formatSessionTime(time: string): string {
  return time.slice(0, 5);
}
