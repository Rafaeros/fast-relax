"use client";

import { useActionState } from "react";
import { Alert, Button, Icon, MaskedInput } from "@/components/ui";
import { collaboratorLoginAction } from "@/features/collaborator-portal/actions/portal.actions";
import { COLLABORATOR_LOGIN_INITIAL_STATE } from "@/features/collaborator-portal/types/portal.types";

/**
 * Entrada do colaborador: o CPF é a credencial inteira.
 *
 * <p>
 * Teclado numérico por padrão (`inputMode`) — digitar CPF no teclado alfabético
 * do celular é atrito puro.
 */
export function CollaboratorLoginForm() {
  const [state, formAction, pending] = useActionState(
    collaboratorLoginAction,
    COLLABORATOR_LOGIN_INITIAL_STATE,
  );

  const fieldErrors = state.fieldErrors ?? {};

  return (
    <form action={formAction} className="flex flex-col gap-5" noValidate>
      {state.status === "error" && state.message && !fieldErrors.cpf && (
        <Alert tone="error">{state.message}</Alert>
      )}

      <MaskedInput
        name="cpf"
        mask="cpf"
        label="Seu CPF"
        placeholder="000.000.000-00"
        inputMode="numeric"
        autoComplete="username"
        autoFocus
        disabled={pending}
        error={fieldErrors.cpf}
        leadingIcon={<Icon name="key" />}
      />

      <Button
        type="submit"
        size="lg"
        fullWidth
        disabled={pending}
        trailingIcon={
          pending ? (
            <Icon name="loader" className="h-4 w-4 animate-spin" />
          ) : (
            <Icon name="arrowRight" className="h-4 w-4" />
          )
        }
      >
        {pending ? "Entrando..." : "Entrar"}
      </Button>

      <p className="text-center text-xs text-ink-tertiary">
        Não consegue entrar? Procure o RH da sua empresa.
      </p>
    </form>
  );
}
