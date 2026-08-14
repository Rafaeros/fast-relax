"use client";

import { useState, useTransition, type FormEvent } from "react";
import { Alert, Button, Icon, Modal } from "@/components/ui";
import { updateDepartmentAction } from "@/features/departments/actions/department.actions";
import { DepartmentFormFields } from "@/features/departments/components/DepartmentFormFields";
import {
  DEPARTMENT_INITIAL_STATE,
  type Department,
} from "@/features/departments/types/department.types";

export type EditDepartmentModalProps = {
  /** `null` mantém o modal fechado — o pai guarda a linha selecionada. */
  department: Department | null;
  onClose: () => void;
  /** Disparado após salvar — a tabela recarrega a partir daqui. */
  onUpdated: () => void;
};

export function EditDepartmentModal({
  department,
  onClose,
  onUpdated,
}: EditDepartmentModalProps) {
  const [state, setState] = useState(DEPARTMENT_INITIAL_STATE);
  const [pending, startTransition] = useTransition();

  const fieldErrors = state.fieldErrors ?? {};

  const close = () => {
    setState(DEPARTMENT_INITIAL_STATE);
    onClose();
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);

    startTransition(async () => {
      const result = await updateDepartmentAction(state, formData);

      if (result.status === "success") {
        setState(DEPARTMENT_INITIAL_STATE);
        onClose();
        onUpdated();
        return;
      }

      setState(result);
    });
  };

  return (
    <Modal
      open={Boolean(department)}
      onClose={close}
      size="sm"
      dismissible={!pending}
      title="Editar departamento"
      description="Desativar mantém o histórico, mas some das opções de cadastro."
      footer={
        <>
          <Button variant="ghost" size="sm" onClick={close} disabled={pending}>
            Cancelar
          </Button>
          <Button
            type="submit"
            form="edit-department-form"
            size="sm"
            disabled={pending}
            trailingIcon={
              pending ? <Icon name="loader" className="h-4 w-4 animate-spin" /> : undefined
            }
          >
            {pending ? "Salvando..." : "Salvar alterações"}
          </Button>
        </>
      }
    >
      {department && (
        <form
          // A key troca junto com o registro: os campos remontam já com os
          // dados de quem foi selecionado, sem arrastar o anterior.
          key={department.id}
          id="edit-department-form"
          onSubmit={handleSubmit}
          className="flex flex-col gap-5"
          noValidate
        >
          <input type="hidden" name="id" value={department.id} />

          {state.status === "error" && state.message && (
            <Alert tone="error">{state.message}</Alert>
          )}

          <DepartmentFormFields
            fieldErrors={fieldErrors}
            disabled={pending}
            department={department}
            showActive
          />
        </form>
      )}
    </Modal>
  );
}
