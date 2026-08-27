"use client";

import { useState, useTransition, type FormEvent } from "react";
import { Button, Icon, Modal, useToast } from "@/components/ui";
import { updateCompanyAction } from "@/features/companies/actions/company.actions";
import { CompanyFormFields } from "@/features/companies/components/CompanyFormFields";
import { COMPANY_INITIAL_STATE, type Company } from "@/features/companies/types/company.types";
import type { State } from "@/features/locations/types/location.types";
import { hasFieldErrors } from "@/lib/forms";

export type EditCompanyModalProps = {
  /** `null` mantém o modal fechado — o pai guarda a linha selecionada. */
  company: Company | null;
  states: State[];
  onClose: () => void;
  /** Disparado após salvar — a tabela recarrega a partir daqui. */
  onUpdated: () => void;
};

export function EditCompanyModal({
  company,
  states,
  onClose,
  onUpdated,
}: EditCompanyModalProps) {
  const [state, setState] = useState(COMPANY_INITIAL_STATE);
  const [pending, startTransition] = useTransition();
  const toast = useToast();

  const fieldErrors = state.fieldErrors ?? {};

  const close = () => {
    setState(COMPANY_INITIAL_STATE);
    onClose();
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);

    startTransition(async () => {
      const result = await updateCompanyAction(state, formData);

      if (result.status === "success") {
        setState(COMPANY_INITIAL_STATE);
        onClose();
        onUpdated();
        if (result.message) toast.success(result.message);
        return;
      }

      // Erro de campo fica no formulário, ao lado do input a corrigir. O recado
      // geral do servidor vai para o toast, que não depende do modal aberto.
      if (result.message && !hasFieldErrors(result.fieldErrors)) toast.error(result.message);
      setState(result);
    });
  };

  return (
    <Modal
      open={Boolean(company)}
      onClose={close}
      size="md"
      dismissible={!pending}
      title="Editar empresa"
      description="Alterar o CNPJ muda o que os colaboradores digitam para entrar."
      footer={
        <>
          <Button variant="ghost" size="sm" onClick={close} disabled={pending}>
            Cancelar
          </Button>
          <Button
            type="submit"
            form="edit-company-form"
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
      {company && (
        <form
          // A key troca junto com o registro: os campos remontam já com os
          // dados de quem foi selecionado, sem arrastar o anterior.
          key={company.id}
          id="edit-company-form"
          onSubmit={handleSubmit}
          className="flex flex-col gap-5"
          noValidate
        >
          <input type="hidden" name="id" value={company.id} />

          <CompanyFormFields
            states={states}
            fieldErrors={fieldErrors}
            disabled={pending}
            company={company}
          />
        </form>
      )}
    </Modal>
  );
}
