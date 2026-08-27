import { apiFetch } from "@/lib/api/http";
import type { ApiResult } from "@/lib/api/api.types";
import type { SpringPage } from "@/lib/api/pagination.types";
import { buildQuery } from "@/lib/api/query";
import { readAccessToken } from "@/features/authentication/services/session.service";
import type {
  CreateUserInput,
  CreatedUser,
  ListUsersParams,
  UpdateUserInput,
  User,
} from "@/features/users/types/user.types";

/**
 * Acesso à API de usuários (`/users`), restrita a ADMIN e RH.
 * Algumas operações são exclusivas de ADMIN (situação e redefinição de senha):
 * a UI as oferece e o backend decide — quem não pode recebe 403 com mensagem.
 */

const RESOURCE = "/users";

/**
 * Página maior que a dos outros recursos: `GET /users` não aceita filtro, então
 * a busca acontece no cliente e depende de ter as linhas carregadas.
 */
export const USERS_PAGE_SIZE = 50;

export async function listUsers(params: ListUsersParams = {}): Promise<ApiResult<SpringPage<User>>> {
  const { page = 0, size = USERS_PAGE_SIZE, sort = "name,asc" } = params;

  return apiFetch<SpringPage<User>>(`${RESOURCE}${buildQuery({ page, size, sort })}`, {
    token: await readAccessToken(),
  });
}

export async function getUser(id: number): Promise<ApiResult<User>> {
  return apiFetch<User>(`${RESOURCE}/${id}`, { token: await readAccessToken() });
}

/** A senha temporária vem na resposta e não pode ser recuperada depois. */
export async function createUser(input: CreateUserInput): Promise<ApiResult<CreatedUser>> {
  return apiFetch<CreatedUser>(RESOURCE, {
    method: "POST",
    body: input,
    token: await readAccessToken(),
  });
}

export async function updateUser(
  id: number,
  input: UpdateUserInput,
): Promise<ApiResult<User>> {
  return apiFetch<User>(`${RESOURCE}/${id}`, {
    method: "PATCH",
    body: input,
    token: await readAccessToken(),
  });
}

/** Exclusivo de ADMIN no backend. */
export async function toggleUserActive(id: number): Promise<ApiResult<User>> {
  return apiFetch<User>(`${RESOURCE}/${id}/toggle-active`, {
    method: "PATCH",
    token: await readAccessToken(),
  });
}

/** Exclusivo de ADMIN. Gera nova senha temporária, exibida uma única vez. */
export async function resetUserPassword(
  id: number,
): Promise<ApiResult<{ temporaryPassword: string }>> {
  return apiFetch<{ temporaryPassword: string }>(`${RESOURCE}/${id}/password`, {
    method: "PATCH",
    token: await readAccessToken(),
  });
}

/** Soft delete no backend — o registro sai das listagens. */
export async function deleteUser(id: number): Promise<ApiResult<null>> {
  return apiFetch<null>(`${RESOURCE}/${id}`, {
    method: "DELETE",
    token: await readAccessToken(),
  });
}

/**
 * Primeiro acesso: troca a senha temporária definida por quem cadastrou.
 *
 * <p>
 * É uma das poucas rotas liberadas enquanto `mustChangePassword` bloqueia o
 * resto da API — sem ela não haveria como sair da senha temporária.
 */
export async function defineFirstAccessPassword(
  newPassword: string,
  confirmNewPassword: string,
): Promise<ApiResult<null>> {
  return apiFetch<null>("/users/me/first-access-password", {
    method: "POST",
    body: { newPassword, confirmNewPassword },
    token: await readAccessToken(),
  });
}

export async function changeMyPassword(
  currentPassword: string,
  newPassword: string,
  confirmNewPassword: string,
): Promise<ApiResult<null>> {
  return apiFetch<null>("/users/me/password", {
    method: "PATCH",
    body: { currentPassword, newPassword, confirmNewPassword },
    token: await readAccessToken(),
  });
}
