"use client";

import { useActionState } from "react";
import { Alert, Button, Icon, Input } from "@/components/ui";
import {
  requestCollaboratorResetAction,
  requestUserResetAction,
} from "@/features/authentication/actions/recovery.actions";
import { RECOVERY_INITIAL_STATE } from "@/features/authentication/types/auth.types";

export type ForgotPasswordFormProps = {
  /**
   * `collaborator` pede também o identificador da empresa: o e-mail dele só é
   * único dentro da empresa, e sem isso a busca seria ambígua.
   */
  audience: "user" | "collaborator";
};

/**
 * Pedido de link de recuperação.
 *
 * <p>
 * Sucesso não confirma que a conta existe — a mensagem é a mesma nos dois casos,
 * porque diferenciá-los transformaria esta tela pública num verificador de
 * "quem trabalha aqui?".
 */
export function ForgotPasswordForm({ audience }: ForgotPasswordFormProps) {
  const collaborator = audience === "collaborator";

  const [state, formAction, pending] = useActionState(
    collaborator ? requestCollaboratorResetAction : requestUserResetAction,
    RECOVERY_INITIAL_STATE,
  );

  const fieldErrors = state.fieldErrors ?? {};

  // Enviado, o formulário some: reenviar em seguida só geraria outro link e
  // invalidaria o primeiro, que talvez já esteja aberto na outra aba.
  if (state.status === "sent") {
    return (
      <Alert tone="success" title="Verifique seu e-mail">
        {state.message}
      </Alert>
    );
  }

  return (
    <form action={formAction} className="flex flex-col gap-5" noValidate>
      {state.status === "error" && state.message && !hasFieldError(fieldErrors) && (
        <Alert tone="error">{state.message}</Alert>
      )}

      {collaborator && (
        <Input
          name="companySlug"
          label="Identificador da empresa"
          placeholder="ex.: lanx"
          autoComplete="organization"
          onInput={(event) => {
            event.currentTarget.value = event.currentTarget.value.toLowerCase();
          }}
          autoFocus={!state.companySlug}
          disabled={pending}
          defaultValue={state.companySlug ?? ""}
          error={fieldErrors.companySlug}
          leadingIcon={<Icon name="building" />}
        />
      )}

      <Input
        name="email"
        type="email"
        label="Seu e-mail"
        placeholder="voce@empresa.com"
        autoComplete="email"
        autoFocus={!collaborator && !state.email}
        disabled={pending}
        // Volta preenchido no erro: o React limpa os campos não controlados
        // depois de uma action, e redigitar o e-mail inteiro por um erro de
        // digitação no identificador da empresa seria atrito puro.
        defaultValue={state.email ?? ""}
        error={fieldErrors.email}
        leadingIcon={<Icon name="mail" />}
      />

      <Button
        type="submit"
        size="lg"
        fullWidth
        disabled={pending}
        trailingIcon={pending ? <Icon name="loader" className="h-4 w-4 animate-spin" /> : undefined}
      >
        {pending ? "Enviando..." : "Enviar link de redefinição"}
      </Button>
    </form>
  );
}

function hasFieldError(fieldErrors: Record<string, string | undefined>): boolean {
  return Object.values(fieldErrors).some(Boolean);
}
