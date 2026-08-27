"use server";

import { redirect } from "next/navigation";
import { onlyDigits } from "@/lib/format";
import { validateNewPassword } from "@/features/authentication/lib/password";
import {
  describeToken,
  requestCollaboratorReset,
  requestUserReset,
  resetPasswordWithToken,
} from "@/features/authentication/services/recovery.service";
import type {
  PasswordFormState,
  RecoveryFieldErrors,
  RecoveryFormState,
  RecoveryTarget,
} from "@/features/authentication/types/auth.types";

/**
 * "Esqueci minha senha" e definição de senha por link.
 *
 * <p>
 * O pedido responde sempre a mesma coisa, exista a conta ou não — é o que a API
 * faz, e repetir aqui evita que a interface vaze pela diferença de mensagem o
 * que o backend se esforça para não revelar.
 */

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

/** Recuperação do painel: só o e-mail. */
export async function requestUserResetAction(
  _previousState: RecoveryFormState,
  formData: FormData,
): Promise<RecoveryFormState> {
  const email = String(formData.get("email") ?? "").trim().toLowerCase();

  if (!EMAIL_PATTERN.test(email)) {
    return {
      status: "error",
      message: "Confira o campo destacado.",
      fieldErrors: { email: "Informe um e-mail válido." },
      email,
    };
  }

  const result = await requestUserReset(email);

  // Falha de rede é o único caso que vira erro: qualquer outra coisa a API já
  // respondeu com a mensagem genérica.
  return result.ok
    ? { status: "sent", message: result.message }
    : { status: "error", message: result.message, email };
}

/** Recuperação do colaborador: CNPJ da empresa mais o e-mail. */
export async function requestCollaboratorResetAction(
  _previousState: RecoveryFormState,
  formData: FormData,
): Promise<RecoveryFormState> {
  // O valor com máscara volta no estado de erro; a API recebe só os dígitos.
  const typedCnpj = String(formData.get("cnpj") ?? "");
  const cnpj = onlyDigits(typedCnpj);
  const email = String(formData.get("email") ?? "").trim().toLowerCase();
  const typed = { cnpj: typedCnpj, email };

  const fieldErrors: RecoveryFieldErrors = {};
  if (cnpj.length !== 14) fieldErrors.cnpj = "Informe os 14 dígitos do CNPJ.";
  if (!EMAIL_PATTERN.test(email)) fieldErrors.email = "Informe um e-mail válido.";

  if (Object.keys(fieldErrors).length > 0) {
    return { status: "error", message: "Confira os campos destacados.", fieldErrors, ...typed };
  }

  const result = await requestCollaboratorReset(cnpj, email);

  return result.ok
    ? { status: "sent", message: result.message }
    : { status: "error", message: result.message, ...typed };
}

/**
 * De quem é o link, para a tela se apresentar antes de pedir a senha.
 *
 * @returns `null` quando o link não vale mais — a tela mostra o aviso em vez de
 *          um formulário que falharia no envio
 */
export async function describeRecoveryTokenAction(token: string): Promise<RecoveryTarget | null> {
  const result = await describeToken(token);
  return result.ok ? result.data : null;
}

/**
 * Define a senha a partir do link e leva ao login certo.
 *
 * <p>
 * Não abre sessão: a pessoa acabou de escolher a senha e entra com ela. Emitir
 * token aqui faria o link de e-mail valer como credencial de acesso, e ele
 * circula por um canal que não controlamos.
 */
export async function resetPasswordAction(
  _previousState: PasswordFormState,
  formData: FormData,
): Promise<PasswordFormState> {
  const token = String(formData.get("token") ?? "");
  const audience = String(formData.get("audience") ?? "USER");
  const newPassword = String(formData.get("newPassword") ?? "");
  const confirmNewPassword = String(formData.get("confirmNewPassword") ?? "");

  const fieldErrors = validateNewPassword(newPassword, confirmNewPassword);
  if (fieldErrors) {
    return { status: "error", message: "Confira os campos destacados.", fieldErrors };
  }

  const result = await resetPasswordWithToken(token, newPassword, confirmNewPassword);

  if (!result.ok) {
    return { status: "error", message: result.message };
  }

  redirect(audience === "COLLABORATOR" ? "/colaborador/entrar?senha=ok" : "/entrar?senha=ok");
}
