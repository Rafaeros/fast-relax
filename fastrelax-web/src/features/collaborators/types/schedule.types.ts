/**
 * Espelha o enum `WorkDay` do backend — segunda a sábado.
 * Domingo fica de fora de propósito: a CHECK constraint da tabela o rejeita.
 */
export const WORK_DAYS = [
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
] as const;

export type WorkDay = (typeof WORK_DAYS)[number];

export const WORK_DAY_LABELS: Record<WorkDay, string> = {
  MONDAY: "Segunda-feira",
  TUESDAY: "Terça-feira",
  WEDNESDAY: "Quarta-feira",
  THURSDAY: "Quinta-feira",
  FRIDAY: "Sexta-feira",
  SATURDAY: "Sábado",
};

export const WORK_DAY_SHORT_LABELS: Record<WorkDay, string> = {
  MONDAY: "Seg",
  TUESDAY: "Ter",
  WEDNESDAY: "Qua",
  THURSDAY: "Qui",
  FRIDAY: "Sex",
  SATURDAY: "Sáb",
};

/** Um dia do horário permitido. Espelha `WorkScheduleItemDTO`. */
export type AllowedWindow = {
  dayOfWeek: WorkDay;
  /** Dia em português, enviado pela API para exibição. */
  dayOfWeekLabel?: string;
  /** "HH:mm" — o backend recebe `LocalTime`. */
  allowedStartTime: string;
  allowedEndTime: string;
};

/** Espelha `CollaboratorWorkScheduleResponseDTO`. */
export type CollaboratorSchedule = AllowedWindow & {
  id: number;
  collaboratorId: number;
  collaboratorName: string | null;
  active: boolean;
  createdAt: string;
  deletedAt: string | null;
};

/** Ordena pela ordem da semana, não pela ordem de inserção. */
export function sortByWeekday<T extends { dayOfWeek: WorkDay }>(windows: T[]): T[] {
  return [...windows].sort(
    (a, b) => WORK_DAYS.indexOf(a.dayOfWeek) - WORK_DAYS.indexOf(b.dayOfWeek),
  );
}
