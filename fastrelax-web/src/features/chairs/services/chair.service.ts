import { apiFetch } from "@/lib/api/http";
import type { ApiResult } from "@/lib/api/api.types";
import type { SpringPage } from "@/lib/api/pagination.types";
import { buildQuery } from "@/lib/api/query";
import { readAccessToken } from "@/features/authentication/services/session.service";
import type {
  Chair,
  ListChairsParams,
  SaveChairInput,
} from "@/features/chairs/types/chair.types";

/**
 * Acesso à API de cadeiras (`/chairs`).
 * Todas as rotas exigem ADMIN ou RH — o token sai do cookie httpOnly, então
 * estas funções só rodam no servidor.
 *
 * O heartbeat (`POST /chairs/heartbeat`) não aparece aqui: quem chama é o
 * ESP32, autenticado por token de dispositivo.
 */

const RESOURCE = "/chairs";

/** Tamanho de página usado pela rolagem infinita da tabela. */
export const CHAIRS_PAGE_SIZE = 20;

export async function listChairs(
  params: ListChairsParams = {},
): Promise<ApiResult<SpringPage<Chair>>> {
  const {
    page = 0,
    size = CHAIRS_PAGE_SIZE,
    sort = "name,asc",
    name,
    active,
    online,
  } = params;

  const query = buildQuery({ page, size, sort, name, active, online });

  return apiFetch<SpringPage<Chair>>(`${RESOURCE}${query}`, {
    token: await readAccessToken(),
  });
}

export async function getChair(id: number): Promise<ApiResult<Chair>> {
  return apiFetch<Chair>(`${RESOURCE}/${id}`, { token: await readAccessToken() });
}

export async function createChair(input: SaveChairInput): Promise<ApiResult<Chair>> {
  return apiFetch<Chair>(RESOURCE, {
    method: "POST",
    body: input,
    token: await readAccessToken(),
  });
}

export async function updateChair(
  id: number,
  input: SaveChairInput,
): Promise<ApiResult<Chair>> {
  return apiFetch<Chair>(`${RESOURCE}/${id}`, {
    method: "PUT",
    body: input,
    token: await readAccessToken(),
  });
}

export async function toggleChairActive(id: number): Promise<ApiResult<Chair>> {
  return apiFetch<Chair>(`${RESOURCE}/${id}/toggle-active`, {
    method: "PATCH",
    token: await readAccessToken(),
  });
}

/** Soft delete no backend — o registro sai das listagens. */
export async function deleteChair(id: number): Promise<ApiResult<null>> {
  return apiFetch<null>(`${RESOURCE}/${id}`, {
    method: "DELETE",
    token: await readAccessToken(),
  });
}
