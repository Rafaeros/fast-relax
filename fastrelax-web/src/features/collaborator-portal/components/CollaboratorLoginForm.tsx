"use client";

import Link from "next/link";
import { useActionState } from "react";
import { Alert, Button, Icon, Input, MaskedInput } from "@/components/ui";
import { collaboratorLoginAction } from "@/features/collaborator-portal/actions/portal.actions";
import { COLLABORATOR_LOGIN_INITIAL_STATE } from "@/features/collaborator-portal/types/portal.types";

/**
 * Entrada do colaborador: CNPJ da empresa, CPF e senha.
 *
 * <p>
 * O CNPJ vem primeiro porque é ele que define de qual empresa é o CPF — o
 * mesmo documento pode estar cadastrado em dois clientes. Os campos numéricos
 * abrem o teclado numérico por padrão (`inputMode`): digitar CPF no teclado
 * alfabético do celular é atrito puro.
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
    <form action={formAction} className="flex flex-col gap-5" noValidate>
      {generalError && <Alert tone="error">{state.message}</Alert>}

      <MaskedInput
        name="cnpj"
        mask="cnpj"
        label="CNPJ da empresa"
        placeholder="00.000.000/0000-00"
        inputMode="numeric"
        autoComplete="organization"
        // Só na primeira carga: depois de um erro o cursor pertence à senha.
        autoFocus={!state.cnpj}
        disabled={pending}
        defaultValue={state.cnpj ?? ""}
        error={fieldErrors.cnpj}
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
        // Foco vai para cá quando já houve tentativa: CNPJ e CPF continuam
        // preenchidos, e a senha é o que resta corrigir.
        autoFocus={state.status === "error" && Boolean(state.cnpj)}
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
