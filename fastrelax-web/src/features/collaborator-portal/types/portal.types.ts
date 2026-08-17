import type { PageParams } from "@/lib/api/pagination.types";

/** Espelha `CollaboratorLoginResponseDTO` do fastrelax-api. */
export type CollaboratorAuthSession = {
  token: string;
  refreshToken: string;
  expiresInSeconds: number;
  collaboratorId: number;
  name: string;
};

/** Espelha `CollaboratorResponseDTO` (`GET /collaborators/me`). */
export type CollaboratorProfile = {
  id: number;
  departmentId: number | null;
  departmentName: string | null;
  name: string;
  /** Só dígitos — a máscara é aplicada na exibição. */
  cpf: string;
  phoneNumber: string;
  active: boolean;
  createdAt: string;
  deletedAt: string | null;
};

export type WorkDay =
  | "MONDAY"
  | "TUESDAY"
  | "WEDNESDAY"
  | "THURSDAY"
  | "FRIDAY"
  | "SATURDAY";

/** Espelha `CollaboratorWorkScheduleResponseDTO`. */
export type AllowedWindow = {
  id: number;
  collaboratorId: number;
  collaboratorName: string;
  dayOfWeek: WorkDay;
  dayOfWeekLabel: string;
  allowedStartTime: string;
  allowedEndTime: string;
  active: boolean;
  createdAt: string;
  deletedAt: string | null;
};

export type SessionStatus = "SCHEDULED" | "STARTED" | "DONE" | "EXPIRED" | "CANCELLED";

/** Espelha `CollaboratorSessionResponseDTO`. */
export type CollaboratorSession = {
  id: number;
  collaboratorId: number;
  collaboratorName: string;
  sessionDate: string;
  startTime: string;
  endTime: string;
  status: SessionStatus;
  statusLabel: string;
  chairId: number | null;
  chairName: string | null;
  startedAt: string | null;
  finishedAt: string | null;
  createdAt: string;
};

/** Espelha `SessionSlotDTO`. */
export type SessionSlot = {
  startTime: string;
  endTime: string;
  available: boolean;
};

/** Espelha `AvailableDayDTO`. */
export type AvailableDay = {
  sessionDate: string;
  dayOfWeek: WorkDay;
  dayOfWeekLabel: string;
  allowedStartTime: string;
  allowedEndTime: string;
  slots: SessionSlot[];
};

/** Espelha `AvailableSlotsResponseDTO`. */
export type AvailableSlots = {
  from: string;
  to: string;
  durationMinutes: number;
  maxAdvanceDays: number;
  days: AvailableDay[];
};

export type ListMySessionsParams = PageParams & {
  status?: SessionStatus;
  from?: string;
  to?: string;
};

/** Espelha `CollaboratorSessionDTO` — sem `endTime`, calculado pelo backend. */
export type BookSessionInput = {
  collaboratorId: number;
  sessionDate: string;
  startTime: string;
};

export type CollaboratorLoginFieldErrors = Partial<Record<"cpf", string>>;

export type CollaboratorLoginFormState = {
  status: "idle" | "error";
  message?: string;
  fieldErrors?: CollaboratorLoginFieldErrors;
};

export const COLLABORATOR_LOGIN_INITIAL_STATE: CollaboratorLoginFormState = { status: "idle" };

/** Resultado das ações que não alimentam formulário (agendar, cancelar, iniciar). */
export type PortalActionResult = {
  ok: boolean;
  message: string;
};

/**
 * Tom visual por status, alinhado aos badges do design system.
 * Fica aqui para tabela, cartão e histórico não divergirem.
 */
export const SESSION_TONE: Record<SessionStatus, "accent" | "success" | "warning" | "neutral"> = {
  SCHEDULED: "accent",
  STARTED: "success",
  DONE: "success",
  EXPIRED: "warning",
  CANCELLED: "neutral",
};

/** Status que ainda ocupam o horário — o app trata os dois como "sessão de agora". */
export const ACTIVE_STATUSES: SessionStatus[] = ["SCHEDULED", "STARTED"];
