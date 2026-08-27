"use client";

import { useActionState } from "react";
import { Alert, Button, Icon, Input } from "@/components/ui";
import {
  PASSWORD_INITIAL_STATE,
  type PasswordFormState,
} from "@/features/authentication/types/auth.types";

export type PasswordSetupFormProps = {
  /**
   * `first-access` não pede a senha atual: a pessoa acabou de usá-la para
   * entrar, e pedir de novo seria só atrito.
   */
  mode: "first-access" | "change";
  /** Server Action que grava — é o que difere painel de app do colaborador. */
  action: (state: PasswordFormState, formData: FormData) => Promise<PasswordFormState>;
};

/**
 * Definição e troca de senha, para qualquer credencial do sistema.
 *
 * <p>
 * Um componente para painel e colaborador, e para os dois modos, porque o
 * formulário é o mesmo menos um campo. Duplicá-lo faria as regras de tamanho e
 * confirmação divergirem com o tempo — exatamente o que o `CredentialService`
 * evita do lado do servidor.
 */
export function PasswordSetupForm({ mode, action }: PasswordSetupFormProps) {
  const firstAccess = mode === "first-access";
  const [state, formAction, pending] = useActionState(action, PASSWORD_INITIAL_STATE);

  const fieldErrors = state.fieldErrors ?? {};

  return (
    <form action={formAction} className="flex flex-col gap-5" noValidate>
      {state.status === "error" && state.message && <Alert tone="error">{state.message}</Alert>}

      {state.status === "success" && state.message && (
        <Alert tone="success">{state.message}</Alert>
      )}

      {!firstAccess && (
        <Input
          name="currentPassword"
          type="password"
          label="Senha atual"
          autoComplete="current-password"
          disabled={pending}
          error={fieldErrors.currentPassword}
          leadingIcon={<Icon name="key" />}
        />
      )}

      <Input
        name="newPassword"
        type="password"
        label="Nova senha"
        autoComplete="new-password"
        autoFocus={firstAccess}
        disabled={pending}
        hint="Entre 8 e 100 caracteres."
        error={fieldErrors.newPassword}
        leadingIcon={<Icon name="key" />}
      />

      <Input
        name="confirmNewPassword"
        type="password"
        label="Confirme a nova senha"
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
        {pending ? "Salvando..." : firstAccess ? "Definir senha" : "Trocar senha"}
      </Button>
    </form>
  );
}
