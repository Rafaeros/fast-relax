"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { signOut } from "@/features/authentication/services/auth.service";
import {
  destroySession,
  readRefreshToken,
} from "@/features/authentication/services/session.service";

/**
 * Encerra a sessão local e revoga o refresh token no backend.
 * A falha da revogação remota não impede o logout local — o cookie sai de
 * qualquer forma, e o token expira sozinho no servidor.
 */
export async function logoutAction(): Promise<void> {
  const refreshToken = await readRefreshToken();

  if (refreshToken) {
    await signOut(refreshToken);
  }

  await destroySession();
  revalidatePath("/", "layout");
  redirect("/entrar");
}
