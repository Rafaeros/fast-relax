import { apiFetch } from "@/lib/api/http";
import type { ApiResult } from "@/lib/api/api.types";
import type { SpringPage } from "@/lib/api/pagination.types";
import { buildQuery } from "@/lib/api/query";
import { readAccessToken } from "@/features/authentication/services/session.service";
import type {
  CreateDepartmentInput,
  Department,
  ListDepartmentsParams,
  UpdateDepartmentInput,
} from "@/features/departments/types/department.types";

/**
 * Acesso à API de departamentos (`/departments`).
 * Todas as rotas exigem ADMIN ou RH — o token sai do cookie httpOnly, então
 * estas funções só rodam no servidor.
 */

const RESOURCE = "/departments";

/** Tamanho de página usado pela rolagem infinita da tabela. */
export const DEPARTMENTS_PAGE_SIZE = 20;

export async function listDepartments(
  params: ListDepartmentsParams = {},
): Promise<ApiResult<SpringPage<Department>>> {
  const { page = 0, size = DEPARTMENTS_PAGE_SIZE, sort = "name,asc", name, active } = params;

  const query = buildQuery({ page, size, sort, name, active });

  return apiFetch<SpringPage<Department>>(`${RESOURCE}${query}`, {
    token: await readAccessToken(),
  });
}

/**
 * Departamentos ativos para preencher selects.
 * Página grande de propósito: a lista alimenta um `<select>`, não uma tabela.
 */
export function listActiveDepartments(): Promise<ApiResult<SpringPage<Department>>> {
  return listDepartments({ page: 0, size: 200, active: true });
}

export async function getDepartment(id: number): Promise<ApiResult<Department>> {
  return apiFetch<Department>(`${RESOURCE}/${id}`, { token: await readAccessToken() });
}

export async function createDepartment(
  input: CreateDepartmentInput,
): Promise<ApiResult<Department>> {
  return apiFetch<Department>(RESOURCE, {
    method: "POST",
    body: input,
    token: await readAccessToken(),
  });
}

export async function updateDepartment(
  id: number,
  input: UpdateDepartmentInput,
): Promise<ApiResult<Department>> {
  return apiFetch<Department>(`${RESOURCE}/${id}`, {
    method: "PUT",
    body: input,
    token: await readAccessToken(),
  });
}

export async function toggleDepartmentActive(id: number): Promise<ApiResult<Department>> {
  return apiFetch<Department>(`${RESOURCE}/${id}/toggle-active`, {
    method: "PATCH",
    token: await readAccessToken(),
  });
}

/** Soft delete no backend — o registro sai das listagens. */
export async function deleteDepartment(id: number): Promise<ApiResult<null>> {
  return apiFetch<null>(`${RESOURCE}/${id}`, {
    method: "DELETE",
    token: await readAccessToken(),
  });
}
