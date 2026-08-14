"use client";

import { useMemo } from "react";
import { Badge, Button, DataTable, Icon, TableIdentity } from "@/components/ui";
import type { DataTableColumn } from "@/components/ui";
import { formatLongDate } from "@/lib/format";
import {
  formatSessionTime,
  SESSION_STATUS_LABELS,
  SESSION_STATUS_TONES,
  type CollaboratorSession,
} from "@/features/sessions/types/session.types";

export type SessionsTableProps = {
  sessions: CollaboratorSession[];
  /** Quando presente, a tabela mostra só o dia selecionado no calendário. */
  selectedDate: string | null;
  onClearDate: () => void;
};

export function SessionsTable({ sessions, selectedDate, onClearDate }: SessionsTableProps) {
  const rows = useMemo(() => {
    const filtered = selectedDate
      ? sessions.filter((session) => session.sessionDate === selectedDate)
      : sessions;

    // Mais recentes primeiro; dentro do dia, na ordem do relógio.
    return [...filtered].sort(
      (a, b) =>
        b.sessionDate.localeCompare(a.sessionDate) || a.startTime.localeCompare(b.startTime),
    );
  }, [sessions, selectedDate]);

  const columns = useMemo<DataTableColumn<CollaboratorSession>[]>(
    () => [
      {
        id: "collaborator",
        header: "Colaborador",
        cell: (row) => <TableIdentity name={row.collaboratorName ?? "—"} />,
      },
      {
        id: "date",
        header: "Data",
        cell: (row) => (
          <span className="text-ink-secondary">{formatLongDate(row.sessionDate)}</span>
        ),
      },
      {
        id: "time",
        header: "Horário",
        hideOnMobile: true,
        cell: (row) => (
          <span className="tabular-nums">
            {formatSessionTime(row.startTime)} às {formatSessionTime(row.endTime)}
          </span>
        ),
      },
      {
        id: "status",
        header: "Situação",
        align: "right",
        cell: (row) => (
          <Badge tone={SESSION_STATUS_TONES[row.status]}>
            {SESSION_STATUS_LABELS[row.status]}
          </Badge>
        ),
      },
    ],
    [],
  );

  return (
    <DataTable
      className="min-h-0"
      title={selectedDate ? `Sessões de ${formatLongDate(selectedDate)}` : "Sessões do mês"}
      description={`${rows.length} ${rows.length === 1 ? "sessão" : "sessões"}`}
      toolbar={
        selectedDate ? (
          <Button
            variant="secondary"
            size="sm"
            onClick={onClearDate}
            leadingIcon={<Icon name="close" className="h-4 w-4" />}
          >
            Ver o mês inteiro
          </Button>
        ) : undefined
      }
      columns={columns}
      rows={rows}
      getRowId={(row) => row.id}
      emptyMessage={
        selectedDate
          ? "Nenhuma sessão neste dia."
          : "Nenhuma sessão registrada no mês selecionado."
      }
    />
  );
}
