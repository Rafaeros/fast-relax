"use server";

import { revalidatePath } from "next/cache";
import { importCollaborators } from "@/features/collaborators/services/import.service";
import type { ImportFormState } from "@/features/collaborators/types/import.types";

/**
 * Importa a planilha de colaboradores.
 *
 * O backend cria e atualiza em massa e devolve o resumo mesmo quando há linhas
 * recusadas — por isso o sucesso aqui significa "arquivo processado", não
 * "tudo importado". As linhas com erro voltam no resultado para correção.
 */
export async function importCollaboratorsAction(
  _previousState: ImportFormState,
  formData: FormData,
): Promise<ImportFormState> {
  const file = formData.get("file");

  if (!(file instanceof File) || file.size === 0) {
    return { status: "error", message: "Selecione a planilha .xlsx para importar." };
  }

  const result = await importCollaborators(file);

  if (!result.ok) {
    return { status: "error", message: result.message };
  }

  revalidatePath("/painel/colaboradores");
  return { status: "success", message: result.message, result: result.data };
}
