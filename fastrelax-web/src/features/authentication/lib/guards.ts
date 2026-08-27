import { redirect } from "next/navigation";
import { panelHomeFor } from "@/config/navigation";
import { getCurrentUser } from "@/features/authentication/services/auth.service";
import type { AuthUser, UserRole } from "@/features/authentication/types/auth.types";

/**
 * Guard das páginas do painel.
 *
 * <p>
 * Esconder o item da sidebar não impede ninguém de digitar a URL. O backend
 * responde 403 de qualquer forma, mas a tela mostraria um erro cru em vez de
 * levar a pessoa para onde ela pode ir — daí o redirecionamento para o primeiro
 * destino do papel, em vez de uma página de acesso negado.
 *
 * @param roles papéis que enxergam a página; ausente aceita qualquer autenticado
 */
export async function requirePanelUser(roles?: UserRole[]): Promise<AuthUser> {
  const user = await getCurrentUser();
  if (!user) redirect("/entrar");

  if (roles && !roles.includes(user.role)) {
    redirect(panelHomeFor(user));
  }

  return user;
}

/** Atalho para as telas que só a equipe da Physical opera. */
export function requirePlatformUser(): Promise<AuthUser> {
  return requirePanelUser(["SYSADMIN"]);
}

/** Atalho para o dia a dia da empresa — o SYSADMIN não alcança dado operacional. */
export function requireCompanyUser(): Promise<AuthUser> {
  return requirePanelUser(["COMPANY_ADMIN", "COMPANY_RH"]);
}
