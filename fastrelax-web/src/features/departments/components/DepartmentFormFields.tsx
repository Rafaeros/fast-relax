"use client";

import { Icon, Input, Select } from "@/components/ui";
import type {
  Department,
  DepartmentFieldErrors,
} from "@/features/departments/types/department.types";

export type DepartmentFormFieldsProps = {
  fieldErrors: DepartmentFieldErrors;
  disabled?: boolean;
  /** Registro em edição — ausente no cadastro. */
  department?: Department;
  /** Mostra o seletor de situação (só faz sentido na edição). */
  showActive?: boolean;
};

/**
 * Campos do departamento, compartilhados entre cadastro e edição.
 * A diferença é só a situação: no cadastro o backend já cria ativo.
 */
export function DepartmentFormFields({
  fieldErrors,
  disabled,
  department,
  showActive = false,
}: DepartmentFormFieldsProps) {
  return (
    <>
      <Input
        name="name"
        label="Nome do departamento"
        placeholder="Ex.: Tecnologia"
        autoComplete="off"
        autoFocus
        maxLength={100}
        disabled={disabled}
        defaultValue={department?.name}
        error={fieldErrors.name}
        leadingIcon={<Icon name="dashboard" />}
      />

      {showActive && (
        <Select
          name="active"
          label="Situação"
          defaultValue={String(department?.active ?? true)}
          disabled={disabled}
          hint="Inativo some das opções ao cadastrar colaboradores."
          options={[
            { label: "Ativo", value: "true" },
            { label: "Inativo", value: "false" },
          ]}
        />
      )}
    </>
  );
}
