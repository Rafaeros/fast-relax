"use server";

import { revalidatePath } from "next/cache";
import { emptyPageSlice, toPageSlice, type PageSlice } from "@/lib/api/pagination.types";
import {
  createDepartment,
  deleteDepartment,
  listDepartments,
  updateDepartment,
} from "@/features/departments/services/department.service";
import {
  mapDepartmentApiErrors,
  validateDepartmentInput,
} from "@/features/departments/schemas/department.schema";
import type {
  Department,
  DepartmentFilter,
  DepartmentFormState,
} from "@/features/departments/types/department.types";
import type { MutationResult } from "@/features/collaborators/types/collaborator.types";

const ROUTE = "/painel/departamentos";

/**
 * Página de departamentos para a rolagem infinita.
 * Falha de API vira lista vazia sem `hasMore`, para a tabela parar de pedir
 * mais em vez de entrar em laço.
 */
export async function fetchDepartmentsPage(
  page: number,
  filter: DepartmentFilter = {},
): Promise<PageSlice<Department>> {
  const result = await listDepartments({ ...filter, page });

  if (!result.ok) {
    return emptyPageSlice<Department>();
  }

  return toPageSlice(result.data);
}

/**
 * Cadastra o departamento.
 * Nome repetido é decidido pelo backend (`BusinessException` → 400): a
 * mensagem dele volta marcada no campo, sem consulta prévia que abriria janela
 * para corrida entre dois cadastros.
 */
export async function createDepartmentAction(
  _previousState: DepartmentFormState,
  formData: FormData,
): Promise<DepartmentFormState> {
  const validation = validateDepartmentInput({ name: String(formData.get("name") ?? "") });

  if (!validation.valid) {
    return {
      status: "error",
      message: "Confira os campos destacados.",
      fieldErrors: validation.fieldErrors,
    };
  }

  const result = await createDepartment({ name: validation.data.name });

  if (!result.ok) {
    const fieldErrors = mapDepartmentApiErrors(result.errors);

    if (Object.keys(fieldErrors).length === 0) {
      fieldErrors.name = result.message;
    }

    return { status: "error", message: result.message, fieldErrors };
  }

  revalidatePath(ROUTE);
  return { status: "success", message: result.message };
}

/** Atualiza nome e situação. O id chega em campo oculto do formulário. */
export async function updateDepartmentAction(
  _previousState: DepartmentFormState,
  formData: FormData,
): Promise<DepartmentFormState> {
  const id = Number(formData.get("id"));

  if (!id || Number.isNaN(id)) {
    return { status: "error", message: "Departamento não identificado." };
  }

  const validation = validateDepartmentInput({
    name: String(formData.get("name") ?? ""),
    active: String(formData.get("active") ?? "true"),
  });

  if (!validation.valid) {
    return {
      status: "error",
      message: "Confira os campos destacados.",
      fieldErrors: validation.fieldErrors,
    };
  }

  const result = await updateDepartment(id, validation.data);

  if (!result.ok) {
    const fieldErrors = mapDepartmentApiErrors(result.errors);

    if (Object.keys(fieldErrors).length === 0) {
      fieldErrors.name = result.message;
    }

    return { status: "error", message: result.message, fieldErrors };
  }

  revalidatePath(ROUTE);
  return { status: "success", message: result.message };
}

export async function deleteDepartmentAction(id: number): Promise<MutationResult> {
  const result = await deleteDepartment(id);
  if (result.ok) revalidatePath(ROUTE);

  return { ok: result.ok, message: result.message };
}
