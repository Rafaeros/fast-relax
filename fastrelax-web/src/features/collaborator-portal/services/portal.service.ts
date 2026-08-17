import { cache } from "react";
import { apiFetch } from "@/lib/api/http";
import type { ApiResult } from "@/lib/api/api.types";
import type { SpringPage } from "@/lib/api/pagination.types";
import { buildQuery } from "@/lib/api/query";
import { readAccessToken } from "@/features/authentication/services/session.service";
import type {
  AllowedWindow,
  AvailableSlots,
  BookSessionInput,
  CollaboratorAuthSession,
  CollaboratorProfile,
  CollaboratorSession,
  ListMySessionsParams,
} from "@/features/collaborator-portal/types/portal.types";

/**
 * Acesso à API pelo app do colaborador.
 *
 * <p>
 * O token sai do cookie httpOnly, então estas funções só rodam no servidor. As
 * rotas de sessão são as mesmas do painel — o backend restringe o resultado ao
 * colaborador logado, sem o app precisar filtrar nada.
 */

export const MY_SESSIONS_PAGE_SIZE = 20;

export function collaboratorSignIn(cpf: string): Promise<ApiResult<CollaboratorAuthSession>> {
  return apiFetch<CollaboratorAuthSession>("/auth/collaborator/login", {
    method: "POST",
    body: { cpf },
  });
}

/**
 * Colaborador autenticado, ou `null` quando o token é de um usuário do painel.
 * `cache` deduplica a chamada dentro da mesma request — layout e página pedem
 * o perfil sem gerar duas idas à API.
 */
export const getCurrentCollaborator = cache(async (): Promise<CollaboratorProfile | null> => {
  const token = await readAccessToken();
  if (!token) return null;

  const result = await apiFetch<CollaboratorProfile>("/collaborators/me", { token });
  return result.ok ? result.data : null;
});

/** Janelas em que este colaborador pode agendar, uma por dia da semana. */
export async function getMyAllowedWindows(): Promise<ApiResult<AllowedWindow[]>> {
  return apiFetch<AllowedWindow[]>("/collaborators/me/schedules", {
    token: await readAccessToken(),
  });
}

/**
 * Sessão em aberto agora. A API devolve 204 quando não há nenhuma, e o cliente
 * HTTP traduz isso para `data: null`.
 */
export async function getMyCurrentSession(): Promise<ApiResult<CollaboratorSession | null>> {
  return apiFetch<CollaboratorSession | null>("/collaborators/sessions/me/current", {
    token: await readAccessToken(),
  });
}

export async function listMySessions(
  params: ListMySessionsParams = {},
): Promise<ApiResult<SpringPage<CollaboratorSession>>> {
  const {
    page = 0,
    size = MY_SESSIONS_PAGE_SIZE,
    sort = "sessionDate,desc",
    status,
    from,
    to,
  } = params;

  const query = buildQuery({ page, size, sort, status, from, to });

  return apiFetch<SpringPage<CollaboratorSession>>(`/collaborators/sessions${query}`, {
    token: await readAccessToken(),
  });
}

/**
 * Grade de horários. Sem `collaboratorId` de propósito: para colaborador logado
 * o backend assume o próprio, e mandar o id abriria espaço para pedir o de
 * outra pessoa e tomar 403.
 */
export async function getAvailableSlots(
  from?: string,
  to?: string,
): Promise<ApiResult<AvailableSlots>> {
  const query = buildQuery({ from, to });

  return apiFetch<AvailableSlots>(`/collaborators/sessions/available-slots${query}`, {
    token: await readAccessToken(),
  });
}

export async function bookSession(
  input: BookSessionInput,
): Promise<ApiResult<CollaboratorSession>> {
  return apiFetch<CollaboratorSession>("/collaborators/sessions", {
    method: "POST",
    body: input,
    token: await readAccessToken(),
  });
}

export async function cancelSession(id: number): Promise<ApiResult<CollaboratorSession>> {
  return apiFetch<CollaboratorSession>(`/collaborators/sessions/${id}/cancel`, {
    method: "PATCH",
    token: await readAccessToken(),
  });
}

/** Aciona a cadeira. Só vale dentro da janela de início configurada pelo RH. */
export async function startSession(id: number): Promise<ApiResult<CollaboratorSession>> {
  return apiFetch<CollaboratorSession>(`/collaborators/sessions/${id}/start`, {
    method: "PATCH",
    token: await readAccessToken(),
  });
}

export async function finishSession(id: number): Promise<ApiResult<CollaboratorSession>> {
  return apiFetch<CollaboratorSession>(`/collaborators/sessions/${id}/finish`, {
    method: "PATCH",
    token: await readAccessToken(),
  });
}
