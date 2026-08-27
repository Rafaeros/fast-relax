"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { validateNewPassword } from "@/features/authentication/lib/password";
import type {
  PasswordFieldErrors,
  PasswordFormState,
} from "@/features/authentication/types/auth.types";
import {
  changeMyPassword,
  defineFirstAccessPassword,
} from "@/features/users/services/user.service";

/**
 * Senha do usuário do painel.
 *
 * <p>
 * Mesmas regras do app do colaborador — a validação vem do mesmo módulo, e o
 * backend usa o mesmo `CredentialService` para os dois. O que muda aqui é só a
 * rota e para onde levar depois.
 */

const PANEL_HOME = "/painel";

/** Primeiro acesso: troca a temporária e destrava o restante do painel. */
export async function firstAccessPasswordAction(
  _previousState: PasswordFormState,
  formData: FormData,
): Promise<PasswordFormState> {
  const newPassword = String(formData.get("newPassword") ?? "");
  const confirmNewPassword = String(formData.get("confirmNewPassword") ?? "");

  const fieldErrors = validateNewPassword(newPassword, confirmNewPassword);
  if (fieldErrors) {
    return { status: "error", message: "Confira os campos destacados.", fieldErrors };
  }

  const result = await defineFirstAccessPassword(newPassword, confirmNewPassword);

  if (!result.ok) {
    return { status: "error", message: result.message };
  }

  // O layout do painel volta a liberar as demais telas assim que o usuário for
  // relido sem `mustChangePassword`.
  revalidatePath("/", "layout");
  redirect(PANEL_HOME);
}

/** Troca da própria senha, conferindo a atual. */
export async function changeOwnPasswordAction(
  _previousState: PasswordFormState,
  formData: FormData,
): Promise<PasswordFormState> {
  const currentPassword = String(formData.get("currentPassword") ?? "");
  const newPassword = String(formData.get("newPassword") ?? "");
  const confirmNewPassword = String(formData.get("confirmNewPassword") ?? "");

  const fieldErrors: PasswordFieldErrors = validateNewPassword(newPassword, confirmNewPassword) ?? {};
  if (!currentPassword) fieldErrors.currentPassword = "Informe sua senha atual.";

  if (Object.keys(fieldErrors).length > 0) {
    return { status: "error", message: "Confira os campos destacados.", fieldErrors };
  }

  const result = await changeMyPassword(currentPassword, newPassword, confirmNewPassword);

  if (!result.ok) {
    return { status: "error", message: result.message };
  }

  // A troca derruba as sessões abertas no backend, inclusive esta: o cookie
  // atual ainda vale até expirar, mas o refresh já não renova.
  return { status: "success", message: result.message };
}
