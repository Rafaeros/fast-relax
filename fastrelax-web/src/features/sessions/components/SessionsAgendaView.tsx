"use client";

import { useState } from "react";
import { Alert, Select } from "@/components/ui";
import { useMonthSessions } from "@/features/sessions/hooks/useMonthSessions";
import {
  SessionsCalendar,
  SessionStatusLegend,
} from "@/features/sessions/components/SessionsCalendar";
import { SessionsTable } from "@/features/sessions/components/SessionsTable";
import {
  SESSION_STATUSES,
  SESSION_STATUS_LABELS,
  type CalendarMonth,
  type CollaboratorSession,
  type SessionStatus,
} from "@/features/sessions/types/session.types";

export type SessionsAgendaViewProps = {
  initialMonth: CalendarMonth;
  initialSessions: CollaboratorSession[];
  /** Mensagem da API quando a primeira carga falhou no servidor. */
  initialError?: string;
};

export function SessionsAgendaView({
  initialMonth,
  initialSessions,
  initialError,
}: SessionsAgendaViewProps) {
  const {
    month,
    sessions,
    status,
    pending,
    error,
    truncated,
    goToPreviousMonth,
    goToNextMonth,
    goToCurrentMonth,
    changeStatus,
  } = useMonthSessions(initialMonth, initialSessions);

  const [selectedDate, setSelectedDate] = useState<string | null>(null);
  const message = error ?? initialError;

  // Trocar de mês zera o dia selecionado: ele não existe na grade nova.
  const withDateReset = (action: () => void) => () => {
    setSelectedDate(null);
    action();
  };

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <SessionStatusLegend />

        <Select
          aria-label="Filtrar por situação"
          value={status ?? ""}
          onChange={(event) =>
            changeStatus(event.target.value ? (event.target.value as SessionStatus) : undefined)
          }
          disabled={pending}
          containerClassName="w-full sm:w-52"
          className="py-2"
        >
          <option value="">Todas as situações</option>
          {SESSION_STATUSES.map((option) => (
            <option key={option} value={option}>
              {SESSION_STATUS_LABELS[option]}
            </option>
          ))}
        </Select>
      </div>

      {message && (
        <Alert tone="error" title="Não foi possível carregar as sessões">
          {message}
        </Alert>
      )}

      {truncated && (
        <Alert tone="warning" title="Mês com muitas sessões">
          O calendário mostra as primeiras 500 sessões do período. Filtre por situação para
          reduzir o volume.
        </Alert>
      )}

      <SessionsCalendar
        month={month}
        sessions={sessions}
        pending={pending}
        selectedDate={selectedDate}
        onSelectDate={setSelectedDate}
        onPreviousMonth={withDateReset(goToPreviousMonth)}
        onNextMonth={withDateReset(goToNextMonth)}
        onCurrentMonth={withDateReset(goToCurrentMonth)}
      />

      <SessionsTable
        sessions={sessions}
        selectedDate={selectedDate}
        onClearDate={() => setSelectedDate(null)}
      />
    </div>
  );
}
