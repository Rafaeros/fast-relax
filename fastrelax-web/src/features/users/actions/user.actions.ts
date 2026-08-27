"use server";

import { revalidatePath } from "next/cache";
import { emptyPageSlice, toPageSlice, type PageSlice } from "@/lib/api/pagination.types";
import {
  createUser,
  deleteUser,
  listUsers,
  resetUserPassword,
  toggleUserActive,
  updateUser,
} from "@/features/users/services/user.service";
import {
  mapUserApiErrors,
  validateCreateUserInput,
  validateUpdateUserInput,
} from "@/features/users/schemas/user.schema";
import type { User, UserFormState } from "@/features/users/types/user.types";
import type { MutationResult } from "@/features/collaborators/types/collaborator.types";

const ROUTE = "/painel/usuarios";

/**
 * Página de usuários para a rolagem infinita.
 * Falha de API vira lista vazia sem `hasMore`, para a tabela parar de pedir
 * mais em vez de entrar em laço.
 */
export async function fetchUsersPage(page: number): Promise<PageSlice<User>> {
  const result = await listUsers({ page });

  if (!result.ok) {
    return emptyPageSlice<User>();
  }

  return toPageSlice(result.data);
}

/**
 * Cadastra o usuário e devolve a senha temporária gerada pelo backend.
 * E-mail repetido é decidido lá (violação de unicidade → 409/400): a mensagem
 * volta marcada no campo, sem consulta prévia que abriria janela para corrida.
 */
export async function createUserAction(
  _previousState: UserFormState,
  formData: FormData,
): Promise<UserFormState> {
  const validation = validateCreateUserInput({
    name: String(formData.get("name") ?? ""),
    email: String(formData.get("email") ?? ""),
    role: String(formData.get("role") ?? ""),
    companyId: String(formData.get("companyId") ?? ""),
  });

  if (!validation.valid) {
    return {
      status: "error",
      message: "Confira os campos destacados.",
      fieldErrors: validation.fieldErrors,
    };
  }

  const result = await createUser(validation.data);

  if (!result.ok) {
    const fieldErrors = mapUserApiErrors(result.errors);

    if (Object.keys(fieldErrors).length === 0 && /e-?mail/i.test(result.message)) {
      fieldErrors.email = result.message;
    }

    return { status: "error", message: result.message, fieldErrors };
  }

  revalidatePath(ROUTE);
  return {
    status: "success",
    message: result.message,
    credential: result.data.credential,
  };
}

/** Atualiza nome e e-mail. O id chega em campo oculto do formulário. */
export async function updateUserAction(
  _previousState: UserFormState,
  formData: FormData,
): Promise<UserFormState> {
  const id = Number(formData.get("id"));

  if (!id || Number.isNaN(id)) {
    return { status: "error", message: "Usuário não identificado." };
  }

  const validation = validateUpdateUserInput({
    name: String(formData.get("name") ?? ""),
    email: String(formData.get("email") ?? ""),
  });

  if (!validation.valid) {
    return {
      status: "error",
      message: "Confira os campos destacados.",
      fieldErrors: validation.fieldErrors,
    };
  }

  const result = await updateUser(id, validation.data);

  if (!result.ok) {
    const fieldErrors = mapUserApiErrors(result.errors);

    if (Object.keys(fieldErrors).length === 0 && /e-?mail/i.test(result.message)) {
      fieldErrors.email = result.message;
    }

    return { status: "error", message: result.message, fieldErrors };
  }

  revalidatePath(ROUTE);
  return { status: "success", message: result.message };
}

/** Ativa/desativa. Exclusivo de ADMIN — RH recebe 403 com mensagem da API. */
export async function toggleUserActiveAction(id: number): Promise<MutationResult> {
  const result = await toggleUserActive(id);
  if (result.ok) revalidatePath(ROUTE);

  return { ok: result.ok, message: result.message };
}

/** Gera nova senha temporária. Exclusivo de ADMIN. */
export async function resetUserPasswordAction(id: number): Promise<UserFormState> {
  const result = await resetUserPassword(id);

  if (!result.ok) {
    return { status: "error", message: result.message };
  }

  return {
    status: "success",
    message: result.message,
    temporaryPassword: result.data.temporaryPassword,
  };
}

export async function deleteUserAction(id: number): Promise<MutationResult> {
  const result = await deleteUser(id);
  if (result.ok) revalidatePath(ROUTE);

  return { ok: result.ok, message: result.message };
}
