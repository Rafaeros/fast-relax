/** Espelha `DailyUsageDTO`. */
export type DailyUsage = {
  /** "YYYY-MM-DD". */
  sessionDate: string;
  totalSessions: number;
  done: number;
  expired: number;
};

/** Espelha `DepartmentUsageDTO`. */
export type DepartmentUsage = {
  departmentId: number | null;
  departmentName: string | null;
  totalSessions: number;
  done: number;
  expired: number;
};

/** Espelha `DashboardResponseDTO` (`GET /dashboard/sessions`). */
export type DashboardSummary = {
  from: string;
  to: string;
  totalSessions: number;
  scheduled: number;
  inProgress: number;
  done: number;
  expired: number;
  cancelled: number;
  /** Percentual de concluídas sobre as encerradas. `null` sem sessões encerradas. */
  attendanceRate: number | null;
  activeCollaborators: number;
  collaboratorsWithoutSchedule: number;
  byDepartment: DepartmentUsage[];
  byDay: DailyUsage[];
};

/** Períodos oferecidos no filtro, em dias corridos até hoje. */
export const DASHBOARD_PERIODS = [7, 30, 90] as const;

export type DashboardPeriod = (typeof DASHBOARD_PERIODS)[number];

export const DEFAULT_DASHBOARD_PERIOD: DashboardPeriod = 30;

export const PERIOD_LABELS: Record<DashboardPeriod, string> = {
  7: "7 dias",
  30: "30 dias",
  90: "90 dias",
};

/** Resumo vazio — usado quando a API falha, para o painel renderizar mesmo assim. */
export function emptyDashboardSummary(): DashboardSummary {
  return {
    from: "",
    to: "",
    totalSessions: 0,
    scheduled: 0,
    inProgress: 0,
    done: 0,
    expired: 0,
    cancelled: 0,
    attendanceRate: null,
    activeCollaborators: 0,
    collaboratorsWithoutSchedule: 0,
    byDepartment: [],
    byDay: [],
  };
}
