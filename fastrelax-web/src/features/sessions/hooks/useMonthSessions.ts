"use client";

import { useState, useTransition } from "react";
import { fetchMonthSessions } from "@/features/sessions/actions/session.actions";
import { currentMonth, shiftMonth } from "@/features/sessions/lib/calendar";
import type {
  CalendarMonth,
  CollaboratorSession,
  SessionStatus,
} from "@/features/sessions/types/session.types";

export type UseMonthSessionsReturn = {
  month: CalendarMonth;
  sessions: CollaboratorSession[];
  status?: SessionStatus;
  pending: boolean;
  error: string | null;
  truncated: boolean;
  goToPreviousMonth: () => void;
  goToNextMonth: () => void;
  goToCurrentMonth: () => void;
  changeStatus: (status?: SessionStatus) => void;
};

/**
 * Sessões do mês em exibição, com navegação e filtro de status.
 * A primeira carga vem do servidor; cada troca chama a Server Action, então o
 * token continua fora do navegador.
 */
export function useMonthSessions(
  initialMonth: CalendarMonth,
  initialSessions: CollaboratorSession[],
): UseMonthSessionsReturn {
  const [month, setMonth] = useState(initialMonth);
  const [sessions, setSessions] = useState(initialSessions);
  const [status, setStatus] = useState<SessionStatus | undefined>(undefined);
  const [error, setError] = useState<string | null>(null);
  const [truncated, setTruncated] = useState(false);
  const [pending, startTransition] = useTransition();

  const load = (nextMonth: CalendarMonth, nextStatus?: SessionStatus) => {
    setMonth(nextMonth);
    setStatus(nextStatus);

    startTransition(async () => {
      const result = await fetchMonthSessions(nextMonth, nextStatus);

      if (!result.ok) {
        setError(result.message);
        setSessions([]);
        return;
      }

      setError(null);
      setSessions(result.sessions);
      setTruncated(result.truncated);
    });
  };

  return {
    month,
    sessions,
    status,
    pending,
    error,
    truncated,
    goToPreviousMonth: () => load(shiftMonth(month, -1), status),
    goToNextMonth: () => load(shiftMonth(month, 1), status),
    goToCurrentMonth: () => load(currentMonth(), status),
    changeStatus: (nextStatus) => load(month, nextStatus),
  };
}
