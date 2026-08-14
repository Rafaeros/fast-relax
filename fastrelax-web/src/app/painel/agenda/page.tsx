import type { Metadata } from "next";
import { SessionsAgendaView } from "@/features/sessions/components/SessionsAgendaView";
import { currentMonth, monthRange } from "@/features/sessions/lib/calendar";
import { listSessions } from "@/features/sessions/services/session.service";

export const metadata: Metadata = {
  title: "Agenda — physical",
};

export default async function AgendaPage() {
  // Mês corrente no servidor: a agenda já chega preenchida, sem piscar vazia.
  const month = currentMonth();
  const { from, to } = monthRange(month);
  const result = await listSessions({ from, to });

  return (
    <SessionsAgendaView
      initialMonth={month}
      initialSessions={result.ok ? (result.data.content ?? []) : []}
      initialError={result.ok ? undefined : result.message}
    />
  );
}
