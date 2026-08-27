"use client";

import { useState, useTransition, type FormEvent } from "react";
import { Button, Icon, Modal, useToast } from "@/components/ui";
import { updateChairAction } from "@/features/chairs/actions/chair.actions";
import { ChairFormFields } from "@/features/chairs/components/ChairFormFields";
import type { FirmwareOption } from "@/features/chairs/types/chair.types";
import { CHAIR_INITIAL_STATE, type Chair } from "@/features/chairs/types/chair.types";
import { hasFieldErrors } from "@/lib/forms";

export type EditChairModalProps = {
  /** Versões do catálogo, para registrar o firmware gravado. */
  firmwares?: FirmwareOption[];
  /** `null` mantém o modal fechado — o pai guarda a linha selecionada. */
  chair: Chair | null;
  onClose: () => void;
  /** Disparado após salvar — a tabela recarrega a partir daqui. */
  onUpdated: () => void;
};

export function EditChairModal({ chair, onClose, onUpdated, firmwares }: EditChairModalProps) {
  const [state, setState] = useState(CHAIR_INITIAL_STATE);
  const [pending, startTransition] = useTransition();
  const toast = useToast();

  const fieldErrors = state.fieldErrors ?? {};

  const close = () => {
    setState(CHAIR_INITIAL_STATE);
    onClose();
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);

    startTransition(async () => {
      const result = await updateChairAction(state, formData);

      if (result.status === "success") {
        setState(CHAIR_INITIAL_STATE);
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
      open={Boolean(chair)}
      onClose={close}
      size="sm"
      dismissible={!pending}
      title="Editar cadeira"
      description="A situação é alterada pelo botão de ativar/desativar na listagem."
      footer={
        <>
          <Button variant="ghost" size="sm" onClick={close} disabled={pending}>
            Cancelar
          </Button>
          <Button
            type="submit"
            form="edit-chair-form"
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
      {chair && (
        <form
          // A key troca junto com o registro: os campos remontam já com os
          // dados de quem foi selecionado, sem arrastar o anterior.
          key={chair.id}
          id="edit-chair-form"
          onSubmit={handleSubmit}
          className="flex flex-col gap-5"
          noValidate
        >
          <input type="hidden" name="id" value={chair.id} />

          <ChairFormFields
            fieldErrors={fieldErrors}
            disabled={pending}
            chair={chair}
            firmwares={firmwares}
          />
        </form>
      )}
    </Modal>
  );
}
