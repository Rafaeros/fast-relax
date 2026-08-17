"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { signOut } from "@/features/authentication/services/auth.service";
import {
  destroySession,
  readRefreshToken,
} from "@/features/authentication/services/session.service";

/** Telas de login conhecidas — o destino do logout sai daqui, nunca do cliente. */
const LOGIN_ROUTES = {
  panel: "/entrar",
  collaborator: "/colaborador/entrar",
} as const;

export type LogoutOrigin = keyof typeof LOGIN_ROUTES;

/**
 * Encerra a sessão local e revoga o refresh token no backend.
 * A falha da revogação remota não impede o logout local — o cookie sai de
 * qualquer forma, e o token expira sozinho no servidor.
 *
 * <p>
 * A origem chega pelo formulário para devolver cada perfil ao próprio login:
 * mandar o colaborador para a tela de e-mail e senha do RH seria um beco sem
 * saída. O valor é validado contra a lista acima — um destino arbitrário vindo
 * do cliente viraria redirecionamento aberto.
 */
export async function logoutAction(formData?: FormData): Promise<void> {
  const origin = String(formData?.get("origin") ?? "panel") as LogoutOrigin;
  const destination = LOGIN_ROUTES[origin] ?? LOGIN_ROUTES.panel;

  const refreshToken = await readRefreshToken();

  if (refreshToken) {
    await signOut(refreshToken);
  }

  await destroySession();
  revalidatePath("/", "layout");
  redirect(destination);
}
