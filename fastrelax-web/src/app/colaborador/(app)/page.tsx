import type { Metadata } from "next";
import { Alert, Card, Icon } from "@/components/ui";
import { CurrentSessionCard } from "@/features/collaborator-portal/components/CurrentSessionCard";
import { MonthlyUsageChart } from "@/features/collaborator-portal/components/MonthlyUsageChart";
import { SessionHistoryList } from "@/features/collaborator-portal/components/SessionHistoryList";
import {
  getMyCurrentSession,
  listMySessions,
} from "@/features/collaborator-portal/services/portal.service";
import type { CollaboratorSession } from "@/features/collaborator-portal/types/portal.types";

export const metadata: Metadata = {
  title: "Início — physical",
};

/** Cartões de números: o que o colaborador confere de relance. */
function summarize(sessions: CollaboratorSession[]) {
  const done = sessions.filter((session) => session.status === "DONE").length;
  const missed = sessions.filter((session) => session.status === "EXPIRED").length;
  const finished = done + missed;

  return {
    done,
    missed,
    // Comparecimento só faz sentido sobre o que já terminou — agendadas e
    // canceladas não entram na conta.
    attendance: finished > 0 ? Math.round((done / finished) * 100) : null,
  };
}

export default async function CollaboratorHomePage() {
  const [currentResult, historyResult] = await Promise.all([
    getMyCurrentSession(),
    listMySessions({ size: 100 }),
  ]);

  const sessions = historyResult.ok ? historyResult.data.content : [];
  const stats = summarize(sessions);

  return (
    <div className="flex flex-col gap-4">
      {!historyResult.ok && (
        <Alert tone="error" title="Não foi possível carregar seu histórico">
          {historyResult.message}
        </Alert>
      )}

      <CurrentSessionCard session={currentResult.ok ? currentResult.data : null} />

      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
        <SummaryTile icon="check" value={String(stats.done)} label="Concluídas" />
        <SummaryTile icon="alert" value={String(stats.missed)} label="Perdidas" />
        <SummaryTile
          icon="heart"
          value={stats.attendance === null ? "—" : `${stats.attendance}%`}
          label="Comparecimento"
          className="max-sm:col-span-2"
        />
      </div>

      <MonthlyUsageChart sessions={sessions} />

      <SessionHistoryList sessions={sessions} limit={5} />
    </div>
  );
}

function SummaryTile({
  icon,
  value,
  label,
  className,
}: {
  icon: "check" | "alert" | "heart";
  value: string;
  label: string;
  className?: string;
}) {
  return (
    <Card padding="md" className={className}>
      <div className="flex flex-col gap-1">
        <Icon name={icon} className="h-4 w-4 text-ink-muted" />
        <span className="font-display text-2xl tabular-nums text-ink-primary">{value}</span>
        <span className="text-xs text-ink-tertiary">{label}</span>
      </div>
    </Card>
  );
}
