"use client";

import { Alert, Badge, Icon } from "@/components/ui";
import {
  WORK_DAY_LABELS,
  WORK_DAY_SHORT_LABELS,
  type AllowedWindow,
} from "@/features/collaborators/types/schedule.types";

export type AllowedWindowsListProps = {
  windows: AllowedWindow[];
  loading?: boolean;
  error?: string | null;
};

/** Exibição somente leitura do horário permitido, por dia da semana. */
export function AllowedWindowsList({ windows, loading, error }: AllowedWindowsListProps) {
  if (loading) {
    return (
      <span className="flex items-center gap-2 text-xs text-ink-tertiary">
        <Icon name="loader" className="h-4 w-4 animate-spin" />
        Carregando horário permitido...
      </span>
    );
  }

  if (error) {
    return <Alert tone="error">{error}</Alert>;
  }

  if (windows.length === 0) {
    return (
      <Alert tone="warning" title="Sem horário permitido">
        Enquanto nenhum dia estiver configurado, este colaborador não consegue agendar sessão.
      </Alert>
    );
  }

  return (
    <ul className="flex flex-col gap-2">
      {windows.map((window) => (
        <li
          key={window.dayOfWeek}
          className="flex items-center gap-3 rounded-control border border-line bg-bg-900 px-3 py-2"
        >
          <Badge tone="neutral">{WORK_DAY_SHORT_LABELS[window.dayOfWeek]}</Badge>
          <span className="flex-1 truncate text-sm text-ink-secondary">
            {WORK_DAY_LABELS[window.dayOfWeek]}
          </span>
          <span className="text-sm tabular-nums text-ink-primary">
            {window.allowedStartTime} às {window.allowedEndTime}
          </span>
        </li>
      ))}
    </ul>
  );
}
