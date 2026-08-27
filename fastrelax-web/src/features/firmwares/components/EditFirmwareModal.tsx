"use client";

import { useState, useTransition, type FormEvent } from "react";
import { Button, Icon, Modal, useToast } from "@/components/ui";
import { updateFirmwareAction } from "@/features/firmwares/actions/firmware.actions";
import { FirmwareFormFields } from "@/features/firmwares/components/FirmwareFormFields";
import { FIRMWARE_INITIAL_STATE, type Firmware } from "@/features/firmwares/types/firmware.types";
import { hasFieldErrors } from "@/lib/forms";

export type EditFirmwareModalProps = {
  /** `null` mantém o modal fechado — o pai guarda a linha selecionada. */
  firmware: Firmware | null;
  onClose: () => void;
  /** Disparado após salvar — a tabela recarrega a partir daqui. */
  onUpdated: () => void;
};

export function EditFirmwareModal({ firmware, onClose, onUpdated }: EditFirmwareModalProps) {
  const [state, setState] = useState(FIRMWARE_INITIAL_STATE);
  const [pending, startTransition] = useTransition();
  const toast = useToast();

  const fieldErrors = state.fieldErrors ?? {};

  const close = () => {
    setState(FIRMWARE_INITIAL_STATE);
    onClose();
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);

    startTransition(async () => {
      const result = await updateFirmwareAction(state, formData);

      if (result.status === "success") {
        setState(FIRMWARE_INITIAL_STATE);
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
      open={Boolean(firmware)}
      onClose={close}
      size="md"
      dismissible={!pending}
      title="Editar firmware"
      description="Os binários enviados substituem a lista inteira da versão."
      footer={
        <>
          <Button variant="ghost" size="sm" onClick={close} disabled={pending}>
            Cancelar
          </Button>
          <Button
            type="submit"
            form="edit-firmware-form"
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
      {firmware && (
        <form
          // A key troca junto com o registro: os campos remontam já com os
          // dados de quem foi selecionado, sem arrastar o anterior.
          key={firmware.id}
          id="edit-firmware-form"
          onSubmit={handleSubmit}
          className="flex flex-col gap-5"
          noValidate
        >
          <input type="hidden" name="id" value={firmware.id} />

          <FirmwareFormFields
            fieldErrors={fieldErrors}
            disabled={pending}
            firmware={firmware}
          />
        </form>
      )}
    </Modal>
  );
}
