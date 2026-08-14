"use client";

import { useState, useTransition, type FormEvent } from "react";
import { Alert, Button, Icon, Modal } from "@/components/ui";
import { createDepartmentAction } from "@/features/departments/actions/department.actions";
import { DepartmentFormFields } from "@/features/departments/components/DepartmentFormFields";
import { DEPARTMENT_INITIAL_STATE } from "@/features/departments/types/department.types";

export type CreateDepartmentModalProps = {
  /** Disparado após o cadastro dar certo — a tabela recarrega a partir daqui. */
  onCreated: () => void;
};

export function CreateDepartmentModal({ onCreated }: CreateDepartmentModalProps) {
  const [open, setOpen] = useState(false);
  const [state, setState] = useState(DEPARTMENT_INITIAL_STATE);
  const [pending, startTransition] = useTransition();
  // Trocar a key remonta o formulário e zera os campos para o próximo cadastro.
  const [formKey, setFormKey] = useState(0);

  const fieldErrors = state.fieldErrors ?? {};

  const resetForm = () => {
    setState(DEPARTMENT_INITIAL_STATE);
    setFormKey((current) => current + 1);
  };

  const openModal = () => {
    resetForm();
    setOpen(true);
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);

    startTransition(async () => {
      const result = await createDepartmentAction(state, formData);

      if (result.status === "success") {
        resetForm();
        setOpen(false);
        onCreated();
        return;
      }

      setState(result);
    });
  };

  return (
    <>
      <Button
        size="sm"
        leadingIcon={<Icon name="dashboard" className="h-4 w-4" />}
        onClick={openModal}
      >
        Novo departamento
      </Button>

      <Modal
        open={open}
        onClose={() => setOpen(false)}
        size="sm"
        dismissible={!pending}
        title="Novo departamento"
        description="Departamentos agrupam colaboradores nos relatórios de uso."
        footer={
          <>
            <Button variant="ghost" size="sm" onClick={() => setOpen(false)} disabled={pending}>
              Cancelar
            </Button>
            <Button
              type="submit"
              form="create-department-form"
              size="sm"
              disabled={pending}
              trailingIcon={
                pending ? <Icon name="loader" className="h-4 w-4 animate-spin" /> : undefined
              }
            >
              {pending ? "Cadastrando..." : "Cadastrar"}
            </Button>
          </>
        }
      >
        <form
          key={formKey}
          id="create-department-form"
          onSubmit={handleSubmit}
          className="flex flex-col gap-5"
          noValidate
        >
          {state.status === "error" && state.message && (
            <Alert tone="error">{state.message}</Alert>
          )}

          <DepartmentFormFields fieldErrors={fieldErrors} disabled={pending} />
        </form>
      </Modal>
    </>
  );
}
