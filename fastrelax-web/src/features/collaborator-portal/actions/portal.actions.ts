"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { onlyDigits } from "@/lib/format";
import { createSession } from "@/features/authentication/services/session.service";
import {
  bookSession,
  cancelSession,
  collaboratorSignIn,
  finishSession,
  startSession,
} from "@/features/collaborator-portal/services/portal.service";
import type {
  CollaboratorLoginFormState,
  PortalActionResult,
} from "@/features/collaborator-portal/types/portal.types";

const HOME = "/colaborador";
const AGENDA = "/colaborador/agenda";

/**
 * Login por CPF. O CPF é a credencial inteira — não há senha, por decisão de
 * produto (rede interna, dado de baixo risco).
 */
export async function collaboratorLoginAction(
  _previousState: CollaboratorLoginFormState,
  formData: FormData,
): Promise<CollaboratorLoginFormState> {
  // A máscara é da interface; a API espera só dígitos.
  const cpf = onlyDigits(String(formData.get("cpf") ?? ""));

  if (cpf.length !== 11) {
    return {
      status: "error",
      message: "Confira o campo destacado.",
      fieldErrors: { cpf: "Informe os 11 dígitos do CPF." },
    };
  }

  const result = await collaboratorSignIn(cpf);

  if (!result.ok) {
    // Mensagens do backend são acionáveis ("acesso desativado", "não
    // encontrado") e chegam prontas para exibição.
    return { status: "error", message: result.message, fieldErrors: { cpf: result.message } };
  }

  await createSession({
    token: result.data.token,
    refreshToken: result.data.refreshToken,
    expiresInSeconds: result.data.expiresInSeconds,
    mustChangePassword: false,
  });

  revalidatePath("/", "layout");
  redirect(HOME);
}

/** Agenda a sessão no horário escolhido na grade. */
export async function bookSessionAction(
  collaboratorId: number,
  sessionDate: string,
  startTime: string,
): Promise<PortalActionResult> {
  const result = await bookSession({ collaboratorId, sessionDate, startTime });

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
