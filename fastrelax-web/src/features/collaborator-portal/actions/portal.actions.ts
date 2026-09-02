"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { onlyDigits } from "@/lib/format";
import { createSession } from "@/features/authentication/services/session.service";
import {
  bookSession,
  cancelSession,
  changeMyPassword,
  collaboratorSignIn,
  defineFirstAccessPassword,
  finishSession,
  startSession,
} from "@/features/collaborator-portal/services/portal.service";
import { validateNewPassword } from "@/features/authentication/lib/password";
import type { PasswordFormState } from "@/features/authentication/types/auth.types";
import type {
  CollaboratorLoginFieldErrors,
  CollaboratorLoginFormState,
  PortalActionResult,
} from "@/features/collaborator-portal/types/portal.types";

const HOME = "/colaborador";
const AGENDA = "/colaborador/agenda";
const DEFINE_PASSWORD = "/colaborador/definir-senha";

/**
 * Login do colaborador: slug da empresa, CPF e senha.
 *
 * O CPF identifica, a senha autentica. O slug entra porque o CPF só é único
 * dentro da empresa — a mesma pessoa pode ser colaboradora de dois clientes.
 */
export async function collaboratorLoginAction(
  _previousState: CollaboratorLoginFormState,
  formData: FormData,
): Promise<CollaboratorLoginFormState> {
  // Os valores originais voltam no estado de erro para o formulário não
  // perder o que foi digitado — a senha fica de fora, e é o único campo que a
  // pessoa refaz.
  const typedSlug = String(formData.get("companySlug") ?? "");
  const typedCpf = String(formData.get("cpf") ?? "");
  const companySlug = typedSlug.trim().toLowerCase();
  const cpf = onlyDigits(typedCpf);
  const password = String(formData.get("password") ?? "");

  const typed = { companySlug: typedSlug, cpf: typedCpf };

  const fieldErrors: CollaboratorLoginFieldErrors = {};
  if (!companySlug) fieldErrors.companySlug = "Informe o identificador da empresa.";
  if (cpf.length !== 11) fieldErrors.cpf = "Informe os 11 dígitos do CPF.";
  if (!password) fieldErrors.password = "Informe sua senha.";

  if (Object.keys(fieldErrors).length > 0) {
    return { status: "error", message: "Confira os campos destacados.", fieldErrors, ...typed };
  }

  const result = await collaboratorSignIn({ companySlug, cpf, password });

  if (!result.ok) {
    // A API responde a mesma mensagem para empresa inexistente, CPF que não
    // está lá, senha errada e acesso desativado — de propósito, para não deixar
    // mapear quem é cliente. Por isso o erro é geral, e não de um campo só.
    return { status: "error", message: result.message, ...typed };
  }

  await createSession({
    token: result.data.token,
    refreshToken: result.data.refreshToken,
    expiresInSeconds: result.data.expiresInSeconds,
    mustChangePassword: result.data.mustChangePassword,
  });

  revalidatePath("/", "layout");

  // Com senha temporária o backend bloqueia o resto da API: mandar para a home
  // só renderizaria uma tela de erro.
  redirect(result.data.mustChangePassword ? DEFINE_PASSWORD : HOME);
}

/** Primeiro acesso: troca a senha temporária entregue pelo RH. */
export async function collaboratorFirstAccessPasswordAction(
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

  // O layout do app volta a liberar as demais telas assim que o perfil for
  // relido sem `mustChangePassword`.
  revalidatePath("/", "layout");
  redirect(HOME);
}

/** Troca da própria senha, conferindo a atual. */
export async function collaboratorChangePasswordAction(
  _previousState: PasswordFormState,
  formData: FormData,
): Promise<PasswordFormState> {
  const currentPassword = String(formData.get("currentPassword") ?? "");
  const newPassword = String(formData.get("newPassword") ?? "");
  const confirmNewPassword = String(formData.get("confirmNewPassword") ?? "");

  const fieldErrors = validateNewPassword(newPassword, confirmNewPassword) ?? {};
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

/** Agenda a sessão no horário escolhido na grade. */
export async function bookSessionAction(
  collaboratorId: number,
  chairId: number,
  sessionDate: string,
  startTime: string,
): Promise<PortalActionResult> {
  const result = await bookSession({ collaboratorId, chairId, sessionDate, startTime });

  if (result.ok) {
    // A grade e o cartão da home mudam juntos: o horário sai de disponível e
    // passa a ser a próxima sessão.
    revalidatePath(AGENDA);
    revalidatePath(HOME);
  }

  return { ok: result.ok, message: result.message };
}

export async function cancelSessionAction(id: number): Promise<PortalActionResult> {
  const result = await cancelSession(id);

  if (result.ok) {
    revalidatePath(HOME);
    revalidatePath(AGENDA);
  }

  return { ok: result.ok, message: result.message };
}

/** Aciona a cadeira. Só passa dentro da janela de início configurada pelo RH. */
export async function startSessionAction(id: number): Promise<PortalActionResult> {
  const result = await startSession(id);
  if (result.ok) revalidatePath(HOME);

  return { ok: result.ok, message: result.message };
}

export async function finishSessionAction(id: number): Promise<PortalActionResult> {
  const result = await finishSession(id);
  if (result.ok) revalidatePath(HOME);

  return { ok: result.ok, message: result.message };
}
