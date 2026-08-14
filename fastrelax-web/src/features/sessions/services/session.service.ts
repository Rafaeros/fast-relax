import { apiFetch } from "@/lib/api/http";
import type { ApiResult } from "@/lib/api/api.types";
import type { SpringPage } from "@/lib/api/pagination.types";
import { buildQuery } from "@/lib/api/query";
import { readAccessToken } from "@/features/authentication/services/session.service";
import type {
  CollaboratorSession,
  ListSessionsParams,
} from "@/features/sessions/types/session.types";

/**
 * Sessões dos colaboradores (`/collaborators/sessions`).
 * O backend limita o que cada perfil enxerga: colaborador logado só vê as
 * próprias; ADMIN e RH veem todas.
 */

const RESOURCE = "/collaborators/sessions";

/**
 * Teto de sessões carregadas por mês na agenda.
 * Um mês inteiro cabe numa página só — paginar dentro do calendário deixaria
 * dias sem os eventos que já existem.
 */
export const SESSIONS_MONTH_PAGE_SIZE = 500;

export async function listSessions(
  params: ListSessionsParams = {},
): Promise<ApiResult<SpringPage<CollaboratorSession>>> {
  const {
    page = 0,
    size = SESSIONS_MONTH_PAGE_SIZE,
    sort = "sessionDate,asc",
    status,
    collaboratorId,
    sessionDate,
    from,
    to,
  } = params;

  const query = buildQuery({
    page,
    size,
    sort,
    status,
    collaboratorId,
    sessionDate,
    from,
    to,
  });

  return apiFetch<SpringPage<CollaboratorSession>>(`${RESOURCE}${query}`, {
    token: await readAccessToken(),
  });
}
