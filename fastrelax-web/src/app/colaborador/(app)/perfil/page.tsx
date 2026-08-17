import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { Alert } from "@/components/ui";
import { ProfileView } from "@/features/collaborator-portal/components/ProfileView";
import {
  getCurrentCollaborator,
  getMyAllowedWindows,
} from "@/features/collaborator-portal/services/portal.service";

export const metadata: Metadata = {
  title: "Perfil — physical",
};

export default async function CollaboratorProfilePage() {
  const collaborator = await getCurrentCollaborator();
  if (!collaborator) redirect("/colaborador/entrar");

  const windowsResult = await getMyAllowedWindows();

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-1">
        <h1 className="font-display text-2xl text-ink-primary">Seu perfil</h1>
        <p className="text-sm text-ink-secondary">
          Dados cadastrais e horários liberados para agendamento.
        </p>
      </div>

      {!windowsResult.ok && (
        <Alert tone="error" title="Não foi possível carregar seus horários">
          {windowsResult.message}
        </Alert>
      )}

      <ProfileView
        profile={collaborator}
        windows={windowsResult.ok ? windowsResult.data : []}
      />
    </div>
  );
}
