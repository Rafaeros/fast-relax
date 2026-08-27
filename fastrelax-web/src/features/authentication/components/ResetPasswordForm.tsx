"use client";

import { useActionState } from "react";
import { Alert, Button, Icon, Input } from "@/components/ui";
import { resetPasswordAction } from "@/features/authentication/actions/recovery.actions";
import {
  PASSWORD_INITIAL_STATE,
  type RecoveryTarget,
} from "@/features/authentication/types/auth.types";

export type ResetPasswordFormProps = {
  /** Valor cru vindo da query string; viaja de volta em campo oculto. */
  token: string;
  target: RecoveryTarget;
};

/**
 * Definição de senha a partir do link recebido por e-mail.
 *
 * <p>
 * Sem campo de senha atual: em um convite ela não existe, e numa recuperação foi
 * esquecida — a prova de identidade foi controlar a caixa de entrada.
 *
 * <p>
 * Separado do {@code PasswordSetupForm}, que é para quem já está logado: aqui a
 * autorização é o token, e ele precisa viajar no corpo.
 */
export function ResetPasswordForm({ token, target }: ResetPasswordFormProps) {
  const [state, formAction, pending] = useActionState(resetPasswordAction, PASSWORD_INITIAL_STATE);

  const fieldErrors = state.fieldErrors ?? {};
  const invite = target.purpose === "INVITE";

  return (
    <form action={formAction} className="flex flex-col gap-5" noValidate>
      {state.status === "error" && state.message && <Alert tone="error">{state.message}</Alert>}

      <input type="hidden" name="token" value={token} />
      {/* Decide para qual login voltar no fim — painel ou app do colaborador. */}
      <input type="hidden" name="audience" value={target.audience} />

      <Input
        name="newPassword"
        type="password"
        label={invite ? "Sua senha" : "Nova senha"}
        autoComplete="new-password"
        autoFocus
        disabled={pending}
        hint="Entre 8 e 100 caracteres."
        error={fieldErrors.newPassword}
        leadingIcon={<Icon name="key" />}
      />

      <Input
        name="confirmNewPassword"
        type="password"
        label="Confirme a senha"
        autoComplete="new-password"
        disabled={pending}
        error={fieldErrors.confirmNewPassword}
        leadingIcon={<Icon name="key" />}
      />

      <Button
        type="submit"
        size="lg"
        fullWidth
        disabled={pending}
        trailingIcon={pending ? <Icon name="loader" className="h-4 w-4 animate-spin" /> : undefined}
      >
        {pending ? "Salvando..." : invite ? "Definir senha e entrar" : "Redefinir senha"}
      </Button>
    </form>
  );
}
