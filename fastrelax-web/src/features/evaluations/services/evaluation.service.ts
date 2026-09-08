import { apiFetch } from "@/lib/api/http";
import type { ApiResult } from "@/lib/api/api.types";
import type { SpringPage } from "@/lib/api/pagination.types";
import { buildQuery } from "@/lib/api/query";
import { readAccessToken } from "@/features/authentication/services/session.service";
import type { CollaboratorSession } from "@/features/collaborator-portal/types/portal.types";
import type {
  CreateEvaluationInput,
  Evaluation,
  EvaluationFilter,
  EvaluationSummary,
  ListEvaluationsParams,
} from "@/features/evaluations/types/evaluation.types";

/**
 * Acesso à API de avaliações (`/evaluations`).
 *
 * <p>
 * A rota de listagem é a mesma para o painel e para o app: o backend restringe
 * o colaborador às próprias respostas e libera a empresa inteira para RH e
 * gestor. O token sai do cookie httpOnly, então estas funções só rodam no
 * servidor.
 */

const RESOURCE = "/evaluations";

/** Tamanho de página da rolagem infinita da tabela do RH. */
export const EVALUATIONS_PAGE_SIZE = 20;

export async function listEvaluations(
  params: ListEvaluationsParams = {},
): Promise<ApiResult<SpringPage<Evaluation>>> {
  const {
    page = 0,
    size = EVALUATIONS_PAGE_SIZE,
    sort = "evaluationDate,desc",
    score,
    collaboratorId,
    departmentId,
    from,
    to,
  } = params;

  const query = buildQuery({ page, size, sort, score, collaboratorId, departmentId, from, to });

  return apiFetch<SpringPage<Evaluation>>(`${RESOURCE}${query}`, {
    token: await readAccessToken(),
  });
}

/** Média, distribuição e adesão no mesmo recorte da listagem. Só RH e gestor. */
export async function getEvaluationSummary(
  filter: EvaluationFilter = {},
): Promise<ApiResult<EvaluationSummary>> {
  const query = buildQuery({ ...filter });

  return apiFetch<EvaluationSummary>(`${RESOURCE}/summary${query}`, {
    token: await readAccessToken(),
  });
}

/**
 * Massagem concluída que ainda espera nota, ou `null`.
 *
 * É o que reabre o modal quando a pessoa fecha sem responder — sem isso, quem
 * saísse do app perderia a chance de avaliar.
 */
export async function getPendingEvaluation(): Promise<ApiResult<CollaboratorSession | null>> {
  return apiFetch<CollaboratorSession | null>(`${RESOURCE}/me/pending`, {
    token: await readAccessToken(),
  });
}

export async function createEvaluation(
  input: CreateEvaluationInput,
): Promise<ApiResult<Evaluation>> {
  return apiFetch<Evaluation>(RESOURCE, {
    method: "POST",
    body: input,
    token: await readAccessToken(),
  });
}
