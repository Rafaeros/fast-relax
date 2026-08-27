"use server";

import { revalidatePath } from "next/cache";
import { emptyPageSlice, toPageSlice, type PageSlice } from "@/lib/api/pagination.types";
import {
  createCompany,
  deleteCompany,
  listCompanies,
  toggleCompanyActive,
  updateCompany,
} from "@/features/companies/services/company.service";
import {
  mapCompanyApiErrors,
  validateCompanyInput,
} from "@/features/companies/schemas/company.schema";
import type { Company, CompanyFormState } from "@/features/companies/types/company.types";
import type { MutationResult } from "@/features/collaborators/types/collaborator.types";

const ROUTE = "/painel/empresas";

/**
 * Página de empresas para a rolagem infinita.
 * Falha de API vira lista vazia sem `hasMore`, para a tabela parar de pedir
 * mais em vez de entrar em laço.
 */
export async function fetchCompaniesPage(page: number): Promise<PageSlice<Company>> {
  const result = await listCompanies({ page });
  return result.ok ? toPageSlice(result.data) : emptyPageSlice<Company>();
}

export async function createCompanyAction(
  _previousState: CompanyFormState,
  formData: FormData,
): Promise<CompanyFormState> {
  const validation = readAndValidate(formData);

  if (!validation.valid) {
    return {
      status: "error",
      message: "Confira os campos destacados.",
      fieldErrors: validation.fieldErrors,
    };
  }

  const result = await createCompany(validation.data);

  if (!result.ok) {
    return errorState(result.message, result.errors);
  }

  revalidatePath(ROUTE);
  return { status: "success", message: result.message };
}

/** Atualiza cadastro e endereço. O id chega em campo oculto do formulário. */
export async function updateCompanyAction(
  _previousState: CompanyFormState,
  formData: FormData,
): Promise<CompanyFormState> {
  const id = Number(formData.get("id"));

  if (!id || Number.isNaN(id)) {
    return { status: "error", message: "Empresa não identificada." };
  }

  const validation = readAndValidate(formData);

  if (!validation.valid) {
    return {
      status: "error",
      message: "Confira os campos destacados.",
      fieldErrors: validation.fieldErrors,
    };
  }

  const result = await updateCompany(id, validation.data);

  if (!result.ok) {
    return errorState(result.message, result.errors);
  }

  revalidatePath(ROUTE);
  return { status: "success", message: result.message };
}

export async function toggleCompanyActiveAction(id: number): Promise<MutationResult> {
  const result = await toggleCompanyActive(id);
  if (result.ok) revalidatePath(ROUTE);

  return { ok: result.ok, message: result.message };
}

export async function deleteCompanyAction(id: number): Promise<MutationResult> {
  const result = await deleteCompany(id);
  if (result.ok) revalidatePath(ROUTE);

  return { ok: result.ok, message: result.message };
}

/** Cadastro e edição mandam o mesmo corpo, então leem o formulário do mesmo jeito. */
function readAndValidate(formData: FormData) {
  return validateCompanyInput({
    cnpj: String(formData.get("cnpj") ?? ""),
    name: String(formData.get("name") ?? ""),
    email: String(formData.get("email") ?? ""),
    phone: String(formData.get("phone") ?? ""),
    cityId: String(formData.get("cityId") ?? ""),
    cep: String(formData.get("cep") ?? ""),
    street: String(formData.get("street") ?? ""),
    number: String(formData.get("number") ?? ""),
    complement: String(formData.get("complement") ?? ""),
    wifiSsid: String(formData.get("wifiSsid") ?? ""),
    wifiPassword: String(formData.get("wifiPassword") ?? ""),
  });
}

/**
 * CNPJ e e-mail duplicados são decididos pelo backend (`BusinessException` →
 * 400): sem campo identificado na resposta, a mensagem cai no CNPJ, que é o
 * conflito mais provável.
 */
function errorState(message: string, errors: string[]): CompanyFormState {
  const fieldErrors = mapCompanyApiErrors(errors);

  if (Object.keys(fieldErrors).length === 0) {
    fieldErrors.cnpj = message;
  }

  return { status: "error", message, fieldErrors };
}
