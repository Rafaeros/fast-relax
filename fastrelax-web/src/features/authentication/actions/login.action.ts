"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { signIn } from "@/features/authentication/services/auth.service";
import { createSession } from "@/features/authentication/services/session.service";
import { validateLoginInput } from "@/features/authentication/schemas/login.schema";
import type { LoginFormState } from "@/features/authentication/types/auth.types";

/**
 * Autentica e abre a sessão. Assinatura de `useActionState`:
 * o estado anterior entra como primeiro argumento e o retorno vira o novo estado.
 *
 * Em caso de sucesso a função não retorna — `redirect` interrompe a execução.
 */
export async function loginAction(
  _previousState: LoginFormState,
  formData: FormData,
): Promise<LoginFormState> {
  // Devolvido em toda saída de erro: o React limpa os campos não controlados
  // depois de uma action, e sem isto errar a senha custava redigitar o e-mail.
  // A senha fica de fora de propósito — reexibi-la a mandaria de volta pela
  // rede, no payload que o navegador guarda.
  const email = String(formData.get("email") ?? "");

  const validation = validateLoginInput({
    email,
    password: String(formData.get("password") ?? ""),
  });

  if (!validation.valid) {
    return {
      status: "error",
      message: "Confira os campos destacados.",
      fieldErrors: validation.fieldErrors,
      email,
    };
  }

  const result = await signIn(validation.data);

  if (!result.ok) {
    // A API já responde com mensagem genérica em 401, sem revelar se o e-mail existe.
    return { status: "error", message: result.message, email };
  }

  await createSession(result.data);

  // Layout e header passam a enxergar o usuário logado.
  revalidatePath("/", "layout");

  // Enquanto a senha temporária não for trocada, o backend bloqueia o resto da
  // API: mandar para o painel só renderizaria uma tela de erro.
  redirect(result.data.mustChangePassword ? "/definir-senha" : "/painel");
}
