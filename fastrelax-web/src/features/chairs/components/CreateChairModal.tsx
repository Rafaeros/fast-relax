"use client";

import { useState, useTransition, type FormEvent } from "react";
import { Button, Icon, Modal, useToast } from "@/components/ui";
import { createChairAction } from "@/features/chairs/actions/chair.actions";
import { ChairFormFields } from "@/features/chairs/components/ChairFormFields";
import type { FirmwareOption } from "@/features/chairs/types/chair.types";
import { CHAIR_INITIAL_STATE } from "@/features/chairs/types/chair.types";
import { hasFieldErrors } from "@/lib/forms";

export type CreateChairModalProps = {
  /** Versões do catálogo, para registrar o firmware gravado. */
  firmwares?: FirmwareOption[];
  /** Disparado após o cadastro dar certo — a tabela recarrega a partir daqui. */
  onCreated: () => void;
};

export function CreateChairModal({ onCreated, firmwares }: CreateChairModalProps) {
  const [open, setOpen] = useState(false);
  const [state, setState] = useState(CHAIR_INITIAL_STATE);
  const [pending, startTransition] = useTransition();
  // Trocar a key remonta o formulário e zera os campos para o próximo cadastro.
  const [formKey, setFormKey] = useState(0);
  const toast = useToast();

  const fieldErrors = state.fieldErrors ?? {};

  const resetForm = () => {
    setState(CHAIR_INITIAL_STATE);
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
      const result = await createChairAction(state, formData);

      if (result.status === "success") {
        resetForm();
        setOpen(false);
        onCreated();
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
    <>
      <Button size="sm" leadingIcon={<Icon name="chair" className="h-4 w-4" />} onClick={openModal}>
        Nova cadeira
      </Button>

      <Modal
        open={open}
        onClose={() => setOpen(false)}
        size="sm"
        dismissible={!pending}
        title="Nova cadeira"
        description="Cadastre o dispositivo antes de ligá-lo: o ESP32 só é aceito se o MAC já existir aqui."
        footer={
          <>
            <Button variant="ghost" size="sm" onClick={() => setOpen(false)} disabled={pending}>
              Cancelar
            </Button>
            <Button
              type="submit"
              form="create-chair-form"
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
          id="create-chair-form"
          onSubmit={handleSubmit}
          className="flex flex-col gap-5"
          noValidate
        >
          <ChairFormFields fieldErrors={fieldErrors} disabled={pending} firmwares={firmwares} />
        </form>
      </Modal>
    </>
  );
}
