"use client";

import { useState, useTransition, type FormEvent } from "react";
import { Button, Icon, Modal, useToast } from "@/components/ui";
import { createCompanyAction } from "@/features/companies/actions/company.actions";
import { CompanyFormFields } from "@/features/companies/components/CompanyFormFields";
import { COMPANY_INITIAL_STATE } from "@/features/companies/types/company.types";
import type { State } from "@/features/locations/types/location.types";
import { hasFieldErrors } from "@/lib/forms";

export type CreateCompanyModalProps = {
  states: State[];
  /** Disparado após o cadastro dar certo — a tabela recarrega a partir daqui. */
  onCreated: () => void;
};

export function CreateCompanyModal({ states, onCreated }: CreateCompanyModalProps) {
  const [open, setOpen] = useState(false);
  const [state, setState] = useState(COMPANY_INITIAL_STATE);
  const [pending, startTransition] = useTransition();
  // Trocar a key remonta o formulário e zera os campos para o próximo cadastro.
  const [formKey, setFormKey] = useState(0);
  const toast = useToast();

  const fieldErrors = state.fieldErrors ?? {};

  const close = () => {
    setOpen(false);
    setState(COMPANY_INITIAL_STATE);
    setFormKey((current) => current + 1);
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);

    startTransition(async () => {
      const result = await createCompanyAction(state, formData);

      if (result.status === "success") {
        close();
        onCreated();
        if (result.message) toast.success(result.message);
        return;
      }

      if (result.message && !hasFieldErrors(result.fieldErrors)) toast.error(result.message);
      setState(result);
    });
  };

  return (
    <>
      <Button
        size="sm"
        leadingIcon={<Icon name="building" className="h-4 w-4" />}
        onClick={() => setOpen(true)}
      >
        Nova empresa
      </Button>

      <Modal
        open={open}
        onClose={close}
        size="md"
        dismissible={!pending}
        title="Nova empresa"
        description="A configuração de sessão é criada junto, com os valores padrão."
        footer={
          <>
            <Button variant="ghost" size="sm" onClick={close} disabled={pending}>
              Cancelar
            </Button>
            <Button
              type="submit"
              form="create-company-form"
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
          id="create-company-form"
          onSubmit={handleSubmit}
          className="flex flex-col gap-5"
          noValidate
        >
          <CompanyFormFields states={states} fieldErrors={fieldErrors} disabled={pending} />
        </form>
      </Modal>
    </>
  );
}
