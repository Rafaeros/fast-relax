"use client";

import { useState } from "react";
import type { CollaboratorSession } from "@/features/collaborator-portal/types/portal.types";
import { EvaluationModal } from "@/features/evaluations/components/EvaluationModal";

export type PendingEvaluationPromptProps = {
  /** Massagem concluída sem nota, vinda do servidor. `null` quando não há. */
  session: CollaboratorSession | null;
};

/**
 * Abre a avaliação assim que existe uma massagem esperando nota.
 *
 * <p>
 * A sessão pendente vem do servidor, não de um evento de tela: é isso que faz o
 * modal aparecer tanto para quem apertou "finalizar" quanto para quem deixou a
 * sessão terminar sozinha no fim do horário e voltou ao app depois — os dois
 * casos passam pela mesma revalidação da home.
 *
 * <p>
 * "Agora não" só silencia aquela sessão nesta visita. A pendência continua no
 * backend por 24h, então a próxima abertura pergunta de novo e depois disso
 * para de perguntar: passado um dia, a nota já seria sobre a lembrança da
 * massagem, não sobre ela.
 */
export function PendingEvaluationPrompt({ session }: PendingEvaluationPromptProps) {
  const [dismissedSessionId, setDismissedSessionId] = useState<number | null>(null);

  if (!session) return null;

  return (
    <EvaluationModal
      open={dismissedSessionId !== session.id}
      session={session}
      onClose={() => setDismissedSessionId(session.id)}
    />
  );
}
