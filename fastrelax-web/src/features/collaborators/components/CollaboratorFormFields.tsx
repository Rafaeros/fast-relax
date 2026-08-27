"use client";

import { Icon, Input, MaskedInput, Select } from "@/components/ui";
import type { CollaboratorFieldErrors } from "@/features/collaborators/schemas/collaborator.schema";
import type { Collaborator } from "@/features/collaborators/types/collaborator.types";
import type { Department } from "@/features/departments/types/department.types";

export type CollaboratorFormFieldsProps = {
  departments: Department[];
  fieldErrors: CollaboratorFieldErrors;
  disabled?: boolean;
  /** Registro em edição — ausente no cadastro. */
  collaborator?: Collaborator;
  /** Mostra o seletor de situação (só faz sentido na edição). */
  showActive?: boolean;
};

/**
 * Campos do colaborador, compartilhados entre cadastro e edição.
 * A diferença entre os dois é só o CPF: obrigatório ao criar, opcional ao
 * editar (em branco mantém o atual, conforme o `UpdateCollaboratorDTO`).
 */
export function CollaboratorFormFields({
  departments,
  fieldErrors,
  disabled,
  collaborator,
  showActive = false,
}: CollaboratorFormFieldsProps) {
  const editing = Boolean(collaborator);
  const noDepartments = departments.length === 0;

  return (
    <>
      <Input
        name="name"
        label="Nome completo"
        placeholder="Nome do colaborador"
        autoComplete="off"
        autoFocus
        disabled={disabled}
        defaultValue={collaborator?.name}
        error={fieldErrors.name}
        leadingIcon={<Icon name="users" />}
      />

      <div className="grid gap-5 sm:grid-cols-2">
        <MaskedInput
          mask="cpf"
          name="cpf"
          label="CPF"
          placeholder="000.000.000-00"
          autoComplete="off"
          disabled={disabled}
          // Na edição o campo já vem com o CPF vinculado. Reenviar o mesmo valor
          // não conta como troca: o backend compara o blind index e ignora.
          defaultValue={collaborator?.cpf}
          hint={
            editing
              ? "Alterar muda o CPF que ele informa no login; a senha continua a mesma."
              : "Único dentro da empresa — o mesmo CPF pode existir em outro cliente."
          }
          error={fieldErrors.cpf}
          leadingIcon={<Icon name="shield" />}
        />

        <MaskedInput
          mask="phone"
          name="phoneNumber"
          label="Telefone"
          placeholder="(11) 90000-0000"
          autoComplete="off"
          disabled={disabled}
          defaultValue={collaborator?.phoneNumber}
          error={fieldErrors.phoneNumber}
          leadingIcon={<Icon name="phone" />}
        />
      </div>

      {/*
        Opcional de propósito: parte do quadro não tem e-mail corporativo, e
        exigir um travaria o cadastro de quem trabalha no chão de fábrica.
        Preenchido, a pessoa recebe convite e define a própria senha — nada
        secreto passa por WhatsApp nem fica anotado na mesa do RH.
      */}
      <Input
        name="email"
        type="email"
        label="E-mail (opcional)"
        placeholder="colaborador@empresa.com"
        autoComplete="off"
        maxLength={180}
        disabled={disabled}
        defaultValue={collaborator?.email ?? ""}
        hint={
          editing
            ? "Em branco remove o e-mail — e com ele a recuperação de senha."
            : "Com e-mail, ele recebe um convite para definir a própria senha."
        }
        error={fieldErrors.email}
        leadingIcon={<Icon name="mail" />}
      />

      <div className={showActive ? "grid gap-5 sm:grid-cols-2" : undefined}>
        <Select
          name="departmentId"
          label="Departamento"
          defaultValue={collaborator?.departmentId ?? ""}
          disabled={disabled || noDepartments}
          error={fieldErrors.departmentId}
        >
          <option value="" disabled>
            Selecione o departamento
          </option>
          {departments.map((department) => (
            <option key={department.id} value={department.id}>
              {department.name}
            </option>
          ))}
        </Select>

        {showActive && (
          <Select
            name="active"
            label="Situação"
            defaultValue={String(collaborator?.active ?? true)}
            disabled={disabled}
            hint="Inativo perde o acesso ao aplicativo."
            options={[
              { label: "Ativo", value: "true" },
              { label: "Inativo", value: "false" },
            ]}
          />
        )}
      </div>
    </>
  );
}
