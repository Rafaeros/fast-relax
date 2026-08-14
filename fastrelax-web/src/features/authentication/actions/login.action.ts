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
  const validation = validateLoginInput({
    email: String(formData.get("email") ?? ""),
    password: String(formData.get("password") ?? ""),
  });

  if (!validation.valid) {
    return {
      status: "error",
      message: "Confira os campos destacados.",
      fieldErrors: validation.fieldErrors,
    };
  }

  const result = await signIn(validation.data);

  if (!result.ok) {
    // A API já responde com mensagem genérica em 401, sem revelar se o e-mail existe.
    return { status: "error", message: result.message };
  }

  await createSession(result.data);

  // Layout e header passam a enxergar o usuário logado.
  revalidatePath("/", "layout");

  // Enquanto a senha temporária não for trocada, o backend bloqueia o resto da API.
  redirect(result.data.mustChangePassword ? "/painel?senha=trocar" : "/painel");
}
