"use client";

import { Alert, Button, Icon, Input } from "@/components/ui";
import { PasswordField } from "@/features/authentication/components/PasswordField";
import { useLoginForm } from "@/features/authentication/hooks/useLoginForm";

export function LoginForm() {
  const { state, formAction, pending } = useLoginForm();
  const fieldErrors = state.fieldErrors ?? {};

  return (
    <form action={formAction} className="flex flex-col gap-5" noValidate>
      {state.status === "error" && state.message && (
        <Alert tone="error">{state.message}</Alert>
      )}

      <Input
        name="email"
        type="email"
        label="E-mail corporativo"
        placeholder="voce@empresa.com"
        autoComplete="email"
        autoFocus
        disabled={pending}
        error={fieldErrors.email}
        leadingIcon={<Icon name="mail" />}
      />

      <PasswordField
        name="password"
        label="Senha"
        placeholder="Sua senha"
        autoComplete="current-password"
        disabled={pending}
        error={fieldErrors.password}
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
        Primeiro acesso? Use a senha temporária enviada pelo administrador.
      </p>
    </form>
  );
}
