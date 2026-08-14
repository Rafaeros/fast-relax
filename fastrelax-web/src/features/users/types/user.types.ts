import type { PageParams } from "@/lib/api/pagination.types";
import type { AuthUser, UserRole } from "@/features/authentication/types/auth.types";

/** `UserResponseDTO` é o mesmo formato do usuário autenticado — um tipo só. */
export type User = AuthUser;

export type { UserRole };

/**
 * Filtro da listagem.
 * `GET /users` aceita só paginação: estes campos são aplicados no cliente,
 * sobre as linhas já carregadas.
 */
export type UserFilter = {
  search?: string;
  role?: UserRole;
  active?: boolean;
};

export type ListUsersParams = PageParams;

/**
 * Espelha `CreateUserRequestDTO`.
 * Sem senha de propósito: o backend gera uma temporária e a devolve uma única
 * vez, para o ADMIN repassar ao usuário.
 */
export type CreateUserInput = {
  name: string;
  email: string;
  role: UserRole;
};

/** Espelha `UpdateUserRequestDTO` — só nome e email. */
export type UpdateUserInput = {
  name: string;
  email?: string;
};

/** Espelha `CreatedUserResponseDTO`. */
export type CreatedUser = {
  user: User;
  temporaryPassword: string;
};

export type UserFieldErrors = Partial<Record<"name" | "email" | "role", string>>;

export type UserFormState = {
  status: "idle" | "success" | "error";
  message?: string;
  fieldErrors?: UserFieldErrors;
  /**
   * Só no cadastro e na redefinição: aparece uma única vez, porque o banco
   * guarda apenas o hash.
   */
  temporaryPassword?: string;
};

export const USER_INITIAL_STATE: UserFormState = { status: "idle" };
