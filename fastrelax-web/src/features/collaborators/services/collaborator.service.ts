import { apiFetch } from "@/lib/api/http";
import type { ApiResult } from "@/lib/api/api.types";
import type { SpringPage } from "@/lib/api/pagination.types";
import { buildQuery } from "@/lib/api/query";
import { readAccessToken } from "@/features/authentication/services/session.service";
import type {
  Collaborator,
  CreateCollaboratorInput,
  CreatedCollaborator,
  ListCollaboratorsParams,
  UpdateCollaboratorInput,
} from "@/features/collaborators/types/collaborator.types";

/**
 * Acesso à API de colaboradores (`/collaborators`).
 * Todas as rotas exigem gestor ou RH da empresa — o token sai do cookie
 * httpOnly, então estas funções só rodam no servidor (Server Component ou
 * Server Action).
 */

const RESOURCE = "/collaborators";

/** Tamanho de página usado pela rolagem infinita da tabela. */
export const COLLABORATORS_PAGE_SIZE = 20;

export async function listCollaborators(
  params: ListCollaboratorsParams = {},
): Promise<ApiResult<SpringPage<Collaborator>>> {
  const {
    page = 0,
    size = COLLABORATORS_PAGE_SIZE,
    sort = "name,asc",
    departmentId,
    name,
    cpf,
    phoneNumber,
    active,
  } = params;

  const query = buildQuery({
    page,
    size,
    sort,
    departmentId,
    name,
    // O backend compara o CPF pelo blind index: precisa ir só com dígitos.
    cpf: cpf?.replace(/\D/g, ""),
    phoneNumber: phoneNumber?.replace(/\D/g, ""),
    active,
  });

  return apiFetch<SpringPage<Collaborator>>(`${RESOURCE}${query}`, {
    token: await readAccessToken(),
  });
}

export async function getCollaborator(id: number): Promise<ApiResult<Collaborator>> {
  return apiFetch<Collaborator>(`${RESOURCE}/${id}`, { token: await readAccessToken() });
}

/**
 * Cadastra e devolve a senha temporária de primeiro acesso — a única vez que
 * ela existe fora do cliente, já que o banco guarda apenas o hash.
 */
export async function createCollaborator(
  input: CreateCollaboratorInput,
): Promise<ApiResult<CreatedCollaborator>> {
  return apiFetch<CreatedCollaborator>(RESOURCE, {
    method: "POST",
    body: { ...input, cpf: input.cpf.replace(/\D/g, "") },
    token: await readAccessToken(),
  });
}

/** Redefinição pelo RH: gera outra temporária e obriga a troca no próximo acesso. */
export async function resetCollaboratorPassword(
  id: number,
): Promise<ApiResult<{ temporaryPassword: string }>> {
  return apiFetch<{ temporaryPassword: string }>(`${RESOURCE}/${id}/password`, {
    method: "PATCH",
    token: await readAccessToken(),
  });
}

export async function updateCollaborator(
  id: number,
  input: UpdateCollaboratorInput,
): Promise<ApiResult<Collaborator>> {
  return apiFetch<Collaborator>(`${RESOURCE}/${id}`, {
    method: "PUT",
    // CPF vai como string vazia quando não muda: é assim que o backend
    // reconhece "manter o atual" sem tratar como troca de credencial.
    body: { ...input, cpf: input.cpf ? input.cpf.replace(/\D/g, "") : "" },
    token: await readAccessToken(),
  });
}

/** Ativa/desativa. Ao desativar, o backend derruba as sessões abertas. */
export async function toggleCollaboratorActive(id: number): Promise<ApiResult<Collaborator>> {
  return apiFetch<Collaborator>(`${RESOURCE}/${id}/toggle-active`, {
    method: "PATCH",
    token: await readAccessToken(),
  });
}

/** Soft delete no backend — o registro sai das listagens. */
export async function deleteCollaborator(id: number): Promise<ApiResult<null>> {
  return apiFetch<null>(`${RESOURCE}/${id}`, {
    method: "DELETE",
    token: await readAccessToken(),
  });
}
