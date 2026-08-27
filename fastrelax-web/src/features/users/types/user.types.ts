import type { PageParams } from "@/lib/api/pagination.types";
import type { AuthUser, CredentialDelivery, UserRole } from "@/features/authentication/types/auth.types";

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
 *
 * Sem senha de propósito: o backend gera uma temporária e a devolve uma única
 * vez, para quem cadastrou repassar ao usuário.
 *
 * `companyId` só é informado pelo SYSADMIN, ao cadastrar o gestor de um
 * cliente. Para quem já opera dentro de uma empresa o backend ignora o campo e
 * usa a do contexto — aceitar o valor do corpo permitiria criar usuário na
 * empresa de outro cliente.
 */
export type CreateUserInput = {
  name: string;
  email: string;
  role: UserRole;
  companyId?: number;
};

/** Espelha `UpdateUserRequestDTO` — só nome e email. */
export type UpdateUserInput = {
  name: string;
  email?: string;
};

/** Espelha `CreatedUserResponseDTO`. */
export type CreatedUser = {
  user: User;
  credential: CredentialDelivery;
};

export type UserFieldErrors = Partial<
  Record<"name" | "email" | "role" | "companyId", string>
>;

export type UserFormState = {
  status: "idle" | "success" | "error";
  message?: string;
  fieldErrors?: UserFieldErrors;
  /**
   * Como o acesso foi entregue no cadastro: convite por e-mail ou senha
   * temporária. Quando é senha, ela aparece uma única vez.
   */
  credential?: CredentialDelivery;
  /** Redefinição por quem administra: sempre senha temporária. */
  temporaryPassword?: string;
};

export const USER_INITIAL_STATE: UserFormState = { status: "idle" };

/**
 * Empresa no select do cadastro de usuário.
 *
 * <p>
 * Só id e nome: a tela de usuários não precisa de endereço nem CNPJ, e mandar
 * o registro inteiro para o cliente carregaria dado de cliente à toa.
 */
export type CompanyOption = {
  id: number;
  name: string;
};
