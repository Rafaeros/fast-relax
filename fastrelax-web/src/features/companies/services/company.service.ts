import { apiFetch } from "@/lib/api/http";
import type { ApiResult } from "@/lib/api/api.types";
import type { SpringPage } from "@/lib/api/pagination.types";
import { buildQuery } from "@/lib/api/query";
import { readAccessToken } from "@/features/authentication/services/session.service";
import type {
  Company,
  ListCompaniesParams,
  SaveCompanyInput,
} from "@/features/companies/types/company.types";

/**
 * Acesso à API de empresas (`/companies`).
 * Todas as rotas exigem SYSADMIN — o token sai do cookie httpOnly, então estas
 * funções só rodam no servidor.
 */

const RESOURCE = "/companies";

/**
 * Página maior que a dos outros recursos: `GET /companies` não aceita filtro,
 * então a busca acontece no cliente e depende de ter as linhas carregadas.
 */
export const COMPANIES_PAGE_SIZE = 50;

export async function listCompanies(
  params: ListCompaniesParams = {},
): Promise<ApiResult<SpringPage<Company>>> {
  const { page = 0, size = COMPANIES_PAGE_SIZE, sort = "name,asc" } = params;

  return apiFetch<SpringPage<Company>>(`${RESOURCE}${buildQuery({ page, size, sort })}`, {
    token: await readAccessToken(),
  });
}

export async function getCompany(id: number): Promise<ApiResult<Company>> {
  return apiFetch<Company>(`${RESOURCE}/${id}`, { token: await readAccessToken() });
}

export async function createCompany(input: SaveCompanyInput): Promise<ApiResult<Company>> {
  return apiFetch<Company>(RESOURCE, {
    method: "POST",
    body: input,
    token: await readAccessToken(),
  });
}

export async function updateCompany(
  id: number,
  input: SaveCompanyInput,
): Promise<ApiResult<Company>> {
  return apiFetch<Company>(`${RESOURCE}/${id}`, {
    method: "PUT",
    body: input,
    token: await readAccessToken(),
  });
}

/**
 * Suspende ou reativa o contrato.
 * Desativar derruba todo mundo da empresa de uma vez — usuários do painel e
 * colaboradores —, porque o backend consulta o estado da empresa a cada
 * autenticação.
 */
export async function toggleCompanyActive(id: number): Promise<ApiResult<Company>> {
  return apiFetch<Company>(`${RESOURCE}/${id}/toggle-active`, {
    method: "PATCH",
    token: await readAccessToken(),
  });
}

/** Soft delete no backend — o registro sai das listagens. */
export async function deleteCompany(id: number): Promise<ApiResult<null>> {
  return apiFetch<null>(`${RESOURCE}/${id}`, {
    method: "DELETE",
    token: await readAccessToken(),
  });
}
