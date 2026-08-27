import type { AuthUser, UserRole } from "@/features/authentication/types/auth.types";

/**
 * As mesmas perguntas que o `AccessGuard` do backend responde, do lado do
 * cliente.
 *
 * <p>
 * A autorização de verdade continua sendo do servidor — o que está aqui só
 * decide o que a interface oferece. Espalhar `role === "COMPANY_ADMIN"` pelas
 * telas produziria o problema clássico: um papel novo aparece e metade das
 * comparações fica para trás.
 */

export const ROLE_LABELS: Record<UserRole, string> = {
  SYSADMIN: "Administrador da plataforma",
  COMPANY_ADMIN: "Gestor da empresa",
  COMPANY_RH: "RH da empresa",
};

/** Rótulo curto, para o badge do header, onde não cabe o nome inteiro. */
export const ROLE_SHORT_LABELS: Record<UserRole, string> = {
  SYSADMIN: "Physical",
  COMPANY_ADMIN: "Gestor",
  COMPANY_RH: "RH",
};

/** Equipe da Physical. */
export function isPlatformTeam(user: Pick<AuthUser, "role"> | null | undefined): boolean {
  return user?.role === "SYSADMIN";
}

/** Gestor do cliente: além de operar, cadastra os usuários do painel da empresa. */
export function administersCompany(user: Pick<AuthUser, "role"> | null | undefined): boolean {
  return user?.role === "COMPANY_ADMIN";
}

/** Gestor ou RH: o dia a dia da empresa. */
export function operatesCompany(user: Pick<AuthUser, "role"> | null | undefined): boolean {
  return user?.role === "COMPANY_ADMIN" || user?.role === "COMPANY_RH";
}

/** Quem pode administrar usuários do painel — o próprio ou os do cliente. */
export function managesUsers(user: Pick<AuthUser, "role"> | null | undefined): boolean {
  return isPlatformTeam(user) || administersCompany(user);
}

export function roleLabel(user: Pick<AuthUser, "role" | "roleLabel"> | null | undefined): string {
  if (!user) return "";
  // O backend já manda o rótulo pronto; a tabela local cobre o caso de uma
  // resposta antiga em cache, sem deixar a tela em branco.
  return user.roleLabel ?? ROLE_LABELS[user.role] ?? user.role;
}
