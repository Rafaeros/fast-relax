"use client";

import Link from "next/link";
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

      {/*
        O e-mail volta do estado depois de um erro: o React limpa os campos não
        controlados quando a action termina, e sem isto errar a senha custava
        redigitar o endereço inteiro. A senha some — é o único campo que a
        pessoa realmente precisa refazer.
      */}
      <Input
        name="email"
        type="email"
        label="E-mail corporativo"
        placeholder="voce@empresa.com"
        autoComplete="email"
        // Só na primeira carga: depois de um erro o cursor pertence à senha.
        autoFocus={!state.email}
        disabled={pending}
        defaultValue={state.email ?? ""}
        error={fieldErrors.email}
        leadingIcon={<Icon name="mail" />}
      />

      <PasswordField
        name="password"
        label="Senha"
        placeholder="Sua senha"
        autoComplete="current-password"
        // Foco vai para cá quando o erro foi de credencial: é onde a correção
        // acontece, e o e-mail já está preenchido.
        autoFocus={state.status === "error" && Boolean(state.email)}
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

      <Link
        href="/esqueci-senha"
        className="text-center text-xs font-semibold text-accent-soft underline underline-offset-2"
      >
        Esqueci minha senha
      </Link>

      <p className="text-center text-xs text-ink-tertiary">
        Primeiro acesso? Procure o convite que chegou no seu e-mail — ou use a senha
        temporária que o administrador repassou.
      </p>
    </form>
  );
}
