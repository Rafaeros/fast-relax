"use client";

import { Icon, Input, Select } from "@/components/ui";
import { ROLE_LABELS } from "@/features/authentication/lib/roles";
import type { UserRole } from "@/features/authentication/types/auth.types";
import type { CompanyOption } from "@/features/users/types/user.types";
import type { User, UserFieldErrors } from "@/features/users/types/user.types";

export type UserFormFieldsProps = {
  fieldErrors: UserFieldErrors;
  disabled?: boolean;
  /** Registro em edição — ausente no cadastro. */
  user?: User;
  /** Papel de quem está cadastrando: define o que ele pode criar. */
  currentRole: UserRole;
  /** Empresas para o SYSADMIN escolher; vazio para os demais papéis. */
  companies?: CompanyOption[];
};

/**
 * Papéis que cada um pode criar.
 *
 * <p>
 * O gestor de um cliente não cria SYSADMIN — seria escalada de privilégio por
 * cadastro comum, e o backend recusa. Oferecer a opção só produziria um 403
 * depois de preencher o formulário inteiro.
 */
const ASSIGNABLE: Record<UserRole, UserRole[]> = {
  SYSADMIN: ["SYSADMIN", "COMPANY_ADMIN", "COMPANY_RH"],
  COMPANY_ADMIN: ["COMPANY_ADMIN", "COMPANY_RH"],
  COMPANY_RH: [],
};

/**
 * Campos do usuário, compartilhados entre cadastro e edição.
 * O perfil só aparece no cadastro: `UpdateUserRequestDTO` aceita apenas nome e
 * e-mail, então oferecer o campo na edição prometeria algo que a API não faz.
 */
export function UserFormFields({
  fieldErrors,
  disabled,
  user,
  currentRole,
  companies = [],
}: UserFormFieldsProps) {
  const editing = Boolean(user);
  const assignable = ASSIGNABLE[currentRole];
  const platform = currentRole === "SYSADMIN";

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
          defaultValue={assignable[assignable.length - 1] ?? ""}
          disabled={disabled}
          error={fieldErrors.role}
          hint="O gestor cadastra usuários do painel; o RH opera o dia a dia."
          options={assignable.map((role) => ({ label: ROLE_LABELS[role], value: role }))}
        />
      )}

      {/*
        Só a equipe da plataforma escolhe a empresa: para os demais papéis ela
        vem do contexto no backend, e um campo aqui só sugeriria um poder que
        eles não têm.
      */}
      {!editing && platform && (
        <Select
          name="companyId"
          label="Empresa"
          disabled={disabled}
          error={fieldErrors.companyId}
          hint="Deixe em branco ao cadastrar alguém da própria Physical."
          options={[
            { label: "Equipe da plataforma (sem empresa)", value: "" },
            ...companies.map((company) => ({ label: company.name, value: String(company.id) })),
          ]}
        />
      )}
    </>
  );
}
