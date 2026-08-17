import { Badge, Card, Icon } from "@/components/ui";
import { formatSessionDate, formatTimeRange } from "@/features/collaborator-portal/lib/format";
import {
  SESSION_TONE,
  type CollaboratorSession,
} from "@/features/collaborator-portal/types/portal.types";

export type SessionHistoryListProps = {
  sessions: CollaboratorSession[];
  /** Corta a lista — a home mostra as últimas, o histórico completo mostra tudo. */
  limit?: number;
  title?: string;
};

/**
 * Histórico em lista, não em tabela: no celular uma tabela com cinco colunas
 * vira rolagem horizontal, e cada sessão tem poucos dados para exibir.
 */
export function SessionHistoryList({
  sessions,
  limit,
  title = "Histórico de agendamentos",
}: SessionHistoryListProps) {
  const visible = limit ? sessions.slice(0, limit) : sessions;

  return (
    <Card padding="none" className="flex flex-col">
      <div className="flex items-center justify-between gap-3 border-b border-line p-4">
        <h2 className="text-sm font-semibold text-ink-primary">{title}</h2>
        {sessions.length > 0 && (
          <span className="text-xs text-ink-tertiary">{sessions.length} no total</span>
        )}
      </div>

      {visible.length === 0 ? (
        <p className="p-8 text-center text-sm text-ink-tertiary">
          Nenhuma massagem registrada ainda.
        </p>
      ) : (
        <ul className="flex flex-col divide-y divide-line">
          {visible.map((session) => (
            <li key={session.id} className="flex items-center gap-3 p-4">
              <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-control bg-surface-hover">
                <Icon name="calendar" className="h-4 w-4 text-ink-muted" />
              </span>

              <div className="flex min-w-0 flex-1 flex-col">
                <span className="truncate text-sm font-medium text-ink-primary">
                  {formatSessionDate(session.sessionDate)}
                </span>
                <span className="text-xs text-ink-tertiary">
                  {formatTimeRange(session.startTime, session.endTime)}
                  {session.chairName ? ` · ${session.chairName}` : ""}
                </span>
              </div>

              <Badge tone={SESSION_TONE[session.status]} className="shrink-0">
                {session.statusLabel}
              </Badge>
            </li>
          ))}
        </ul>
      )}
    </Card>
  );
}
