import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { Alert } from "@/components/ui";
import { BookingView } from "@/features/collaborator-portal/components/BookingView";
import {
  getAvailableSlots,
  getCurrentCollaborator,
} from "@/features/collaborator-portal/services/portal.service";

export const metadata: Metadata = {
  title: "Agendar — physical",
};

export default async function CollaboratorAgendaPage() {
  const collaborator = await getCurrentCollaborator();
  if (!collaborator) redirect("/colaborador/entrar");

  // Sem intervalo: a API devolve de hoje até o limite de antecedência vigente,
  // já sem os horários que passaram.
  const result = await getAvailableSlots();

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-1">
        <h1 className="font-display text-2xl text-ink-primary">Agendar massagem</h1>
        <p className="text-sm text-ink-secondary">
          Escolha o dia e o horário dentro da sua janela permitida.
        </p>
      </div>

      {result.ok ? (
        <BookingView collaboratorId={collaborator.id} slots={result.data} />
      ) : (
        <Alert tone="error" title="Não foi possível carregar os horários">
          {result.message}
        </Alert>
      )}
    </div>
  );
}
