"use client";

import { useEffect, useState } from "react";
import { fetchCollaboratorScheduleAction } from "@/features/collaborators/actions/schedule.actions";
import { sortByWeekday, type AllowedWindow } from "@/features/collaborators/types/schedule.types";

export type UseCollaboratorScheduleReturn = {
  windows: AllowedWindow[];
  loading: boolean;
  error: string | null;
};

/**
 * Horário permitido do colaborador selecionado.
 *
 * Busca sob demanda: a listagem não traz essa informação, e carregá-la por
 * linha custaria uma requisição por colaborador em tela.
 */
export function useCollaboratorSchedule(
  collaboratorId: number | null,
): UseCollaboratorScheduleReturn {
  const [state, setState] = useState<{
    id: number | null;
    windows: AllowedWindow[];
    error: string | null;
  }>({ id: null, windows: [], error: null });

  useEffect(() => {
    if (collaboratorId === null) return;

    // Trocar de colaborador com a busca em voo descartaria a resposta certa.
    let cancelled = false;

    void (async () => {
      const result = await fetchCollaboratorScheduleAction(collaboratorId);
      if (cancelled) return;

      setState({
        id: collaboratorId,
        windows: result.ok ? sortByWeekday(result.windows) : [],
        error: result.ok ? null : result.message,
      });
    })();

    return () => {
      cancelled = true;
    };
  }, [collaboratorId]);

  // Derivado do estado, não sincronizado por efeito: enquanto o id carregado
  // não for o pedido, ainda está carregando.
  const loading = collaboratorId !== null && state.id !== collaboratorId;

  return {
    windows: loading ? [] : state.windows,
    loading,
    error: loading ? null : state.error,
  };
}
