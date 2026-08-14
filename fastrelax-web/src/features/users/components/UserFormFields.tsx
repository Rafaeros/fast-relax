"use client";

import { Icon, Input, Select } from "@/components/ui";
import type { User, UserFieldErrors } from "@/features/users/types/user.types";

export type UserFormFieldsProps = {
  fieldErrors: UserFieldErrors;
  disabled?: boolean;
  /** Registro em edição — ausente no cadastro. */
  user?: User;
};

/**
 * Campos do usuário, compartilhados entre cadastro e edição.
 * O perfil só aparece no cadastro: `UpdateUserRequestDTO` aceita apenas nome e
 * e-mail, então oferecer o campo na edição prometeria algo que a API não faz.
 */
export function UserFormFields({ fieldErrors, disabled, user }: UserFormFieldsProps) {
  const editing = Boolean(user);

  return (
    <>
      <Input
        name="name"
        label="Nome completo"
        placeholder="Nome do usuário"
        autoComplete="off"
        autoFocus
        maxLength={editing ? 100 : 120}
        disabled={disabled}
        defaultValue={user?.name}
        error={fieldErrors.name}
        leadingIcon={<Icon name="users" />}
      />

      <Input
        name="email"
        type="email"
        label="E-mail corporativo"
        placeholder="usuario@empresa.com"
        autoComplete="off"
        maxLength={180}
        disabled={disabled}
        defaultValue={user?.email}
        hint={editing ? "É com este e-mail que a pessoa entra no painel." : undefined}
        error={fieldErrors.email}
        leadingIcon={<Icon name="mail" />}
      />

      {!editing && (
        <Select
          name="role"
          label="Perfil de acesso"
          defaultValue="RH"
          disabled={disabled}
          error={fieldErrors.role}
          hint="ADMIN também gerencia usuários e redefine senhas."
          options={[
            { label: "RH", value: "RH" },
            { label: "ADMIN", value: "ADMIN" },
          ]}
        />
      )}
    </>
  );
}
