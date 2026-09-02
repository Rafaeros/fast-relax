"use client";

import { useMemo, useState } from "react";
import { Icon, Input, MaskedInput, Select } from "@/components/ui";
import { QuickCreateDepartmentButton } from "@/features/departments/components/QuickCreateDepartmentButton";
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
  /**
   * Avisa o pai quando a lista efetiva de departamentos muda — inclui os
   * cadastrados pelo "+" sem sair deste formulário. Quem mostra o aviso de
   * "nenhum departamento" usa isto para não travar depois de um cadastro rápido.
   */
  onDepartmentsChange?: (departments: Department[]) => void;
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
  onDepartmentsChange,
}: CollaboratorFormFieldsProps) {
  const editing = Boolean(collaborator);
  // Cadastrados pelo "+" nesta sessão do formulário, além dos que já vieram do
  // pai — a lista do pai só se atualiza numa próxima navegação/revalidação.
  const [extraDepartments, setExtraDepartments] = useState<Department[]>([]);
  const allDepartments = useMemo(() => {
    const extraIds = new Set(extraDepartments.map((department) => department.id));
    return [...departments.filter((department) => !extraIds.has(department.id)), ...extraDepartments];
  }, [departments, extraDepartments]);
  const [departmentId, setDepartmentId] = useState(
    collaborator?.departmentId ? String(collaborator.departmentId) : "",
  );
  const noDepartments = allDepartments.length === 0;

  const handleDepartmentCreated = (department: Department) => {
    setExtraDepartments((current) => [...current, department]);
    setDepartmentId(String(department.id));
    onDepartmentsChange?.([...allDepartments, department]);
  };

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
          label="Telefone (opcional)"
          placeholder="(11) 90000-0000"
          autoComplete="off"
          disabled={disabled}
          defaultValue={collaborator?.phoneNumber ?? ""}
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
        <div className="flex flex-col gap-1.5">
          <div className="flex items-center justify-between">
            <label
              htmlFor="collaborator-department-id"
              className="text-xs font-semibold tracking-wide text-ink-secondary"
            >
              Departamento
            </label>
            <QuickCreateDepartmentButton disabled={disabled} onCreated={handleDepartmentCreated} />
          </div>

          <Select
            id="collaborator-department-id"
            name="departmentId"
            value={departmentId}
            onChange={(event) => setDepartmentId(event.target.value)}
            disabled={disabled || noDepartments}
            error={fieldErrors.departmentId}
          >
            <option value="" disabled>
              Selecione o departamento
            </option>
            {allDepartments.map((department) => (
              <option key={department.id} value={department.id}>
                {department.name}
              </option>
            ))}
          </Select>
        </div>

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
