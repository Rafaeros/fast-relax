"use server";

import { revalidatePath } from "next/cache";
import { emptyPageSlice, toPageSlice, type PageSlice } from "@/lib/api/pagination.types";
import {
  createEvaluation,
  getEvaluationSummary,
  listEvaluations,
} from "@/features/evaluations/services/evaluation.service";
import {
  EVALUATION_COMMENTS_MAX_LENGTH,
  type CreateEvaluationInput,
  type Evaluation,
  type EvaluationActionResult,
  type EvaluationFilter,
  type EvaluationSummary,
} from "@/features/evaluations/types/evaluation.types";

const PANEL_ROUTE = "/painel/avaliacoes";
const COLLABORATOR_HOME = "/colaborador";

/**
 * Página de avaliações para a rolagem infinita.
 * Falha de API vira lista vazia sem `hasMore`, para a tabela parar de pedir
 * mais em vez de entrar em laço.
 */
export async function fetchEvaluationsPage(
  page: number,
  filter: EvaluationFilter = {},
): Promise<PageSlice<Evaluation>> {
  const result = await listEvaluations({ ...filter, page });

  if (!result.ok) {
    return emptyPageSlice<Evaluation>();
  }

  return toPageSlice(result.data);
}

/**
 * Envia a nota da massagem que acabou.
 *
 * <p>
 * Revalida a home do colaborador: é de lá que sai a sessão pendente de
 * avaliação, e sem isso o modal reabriria depois de respondido.
 */
export async function submitEvaluationAction(
  input: CreateEvaluationInput,
): Promise<EvaluationActionResult> {
  if (!input.score || input.score < 1 || input.score > 5) {
    return { ok: false, message: "Escolha uma nota de 1 a 5." };
  }

  const comments = input.comments?.trim();

  if (comments && comments.length > EVALUATION_COMMENTS_MAX_LENGTH) {
    return {
      ok: false,
      message: `O comentário pode ter no máximo ${EVALUATION_COMMENTS_MAX_LENGTH} caracteres.`,
    };
  }

  const result = await createEvaluation({
    sessionId: input.sessionId,
    score: input.score,
    // Comentário em branco não vai como string vazia: ausência de comentário é
    // o normal, e o backend guarda nulo.
    comments: comments ? comments : undefined,
  });

  if (result.ok) {
    revalidatePath(COLLABORATOR_HOME);
    revalidatePath(PANEL_ROUTE);
  }

  return { ok: result.ok, message: result.message };
}

/**
 * Resumo para o recorte em vigor na tela.
 *
 * <p>
 * Existe porque os números do topo e a tabela têm que falar do mesmo conjunto:
 * manter o resumo preso ao carregamento inicial faria a média continuar
 * respondendo por todas as avaliações enquanto a lista já mostraria só as notas
 * baixas do mês.
 */
export async function fetchEvaluationSummaryAction(
  filter: EvaluationFilter = {},
): Promise<EvaluationSummary | null> {
  const result = await getEvaluationSummary(filter);
  return result.ok ? result.data : null;
}
