"use client";

import Link from "next/link";
import { useActionState } from "react";
import { Alert, Button, Icon, Input, MaskedInput } from "@/components/ui";
import { LOGIN_FORM } from "@/features/authentication/lib/login-layout";
import { collaboratorLoginAction } from "@/features/collaborator-portal/actions/portal.actions";
import { COLLABORATOR_LOGIN_INITIAL_STATE } from "@/features/collaborator-portal/types/portal.types";

/**
 * Entrada do colaborador: slug da empresa, CPF e senha.
 *
 * <p>
 * O slug vem primeiro porque é ele que define de qual empresa é o CPF — o
 * mesmo documento pode estar cadastrado em dois clientes. O CPF abre o teclado
 * numérico por padrão (`inputMode`): digitá-lo no teclado alfabético do
 * celular é atrito puro.
 */
export function CollaboratorLoginForm() {
  const [state, formAction, pending] = useActionState(
    collaboratorLoginAction,
    COLLABORATOR_LOGIN_INITIAL_STATE,
  );

  const fieldErrors = state.fieldErrors ?? {};
  // A recusa de credencial vem sem campo marcado — a API responde a mesma
  // mensagem para empresa, CPF e senha, para não deixar descobrir qual errou.
  const generalError = state.status === "error" && state.message && !hasFieldError(fieldErrors);

  return (
    <form action={formAction} className={LOGIN_FORM} noValidate>
      {generalError && <Alert tone="error">{state.message}</Alert>}

      <Input
        name="companySlug"
        label="Identificador da empresa"
        placeholder="ex.: lanx"
        autoComplete="organization"
        // Minúsculas de propósito: é assim que o backend compara, e digitar já
        // nesse formato evita o "por que não funciona?" de um "Lanx" com maiúscula.
        onInput={(event) => {
          event.currentTarget.value = event.currentTarget.value.toLowerCase();
        }}
        // Só na primeira carga: depois de um erro o cursor pertence à senha.
        autoFocus={!state.companySlug}
        disabled={pending}
        defaultValue={state.companySlug ?? ""}
        error={fieldErrors.companySlug}
        leadingIcon={<Icon name="building" />}
      />

      <MaskedInput
        name="cpf"
        mask="cpf"
        label="Seu CPF"
        placeholder="000.000.000-00"
        inputMode="numeric"
        autoComplete="username"
        disabled={pending}
        defaultValue={state.cpf ?? ""}
        error={fieldErrors.cpf}
        leadingIcon={<Icon name="users" />}
      />

      <Input
        name="password"
        type="password"
        label="Senha"
        placeholder="Sua senha"
        autoComplete="current-password"
        // Foco vai para cá quando já houve tentativa: empresa e CPF continuam
        // preenchidos, e a senha é o que resta corrigir.
        autoFocus={state.status === "error" && Boolean(state.companySlug)}
        disabled={pending}
        error={fieldErrors.password}
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

      <Link
        href="/colaborador/esqueci-senha"
        className="text-center text-xs font-semibold text-accent-soft underline underline-offset-2"
      >
        Esqueci minha senha
      </Link>

      <p className="text-center text-xs text-ink-tertiary">
        Primeiro acesso? Veja o convite no seu e-mail, ou peça a senha ao RH.
      </p>
    </form>
  );
}

function hasFieldError(fieldErrors: Record<string, string | undefined>): boolean {
  return Object.values(fieldErrors).some(Boolean);
}
