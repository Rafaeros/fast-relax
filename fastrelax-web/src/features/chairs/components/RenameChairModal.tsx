"use client";

import { useState, useTransition, type FormEvent } from "react";
import { Button, Icon, Input, Modal, useToast } from "@/components/ui";
import { renameChairAction } from "@/features/chairs/actions/chair.actions";
import { CHAIR_INITIAL_STATE, type Chair } from "@/features/chairs/types/chair.types";
import { hasFieldErrors } from "@/lib/forms";

export type RenameChairModalProps = {
  /** `null` mantém o modal fechado — o pai guarda a linha selecionada. */
  chair: Chair | null;
  onClose: () => void;
  /** Disparado após salvar — a tabela recarrega a partir daqui. */
  onUpdated: () => void;
};

/**
 * Edição do RH/gestor da empresa: só o nome.
 *
 * <p>
 * MAC, IP, porta, firmware e BSSID são propriedade física do equipamento —
 * quem instala e substitui hardware é a equipe da plataforma. Um modal à
 * parte, em vez de desabilitar campos do {@code EditChairModal}, porque os
 * outros valores nem deveriam chegar ao painel do cliente.
 */
export function RenameChairModal({ chair, onClose, onUpdated }: RenameChairModalProps) {
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
      const result = await renameChairAction(state, formData);

      if (result.status === "success") {
        setState(CHAIR_INITIAL_STATE);
        onClose();
        onUpdated();
        if (result.message) toast.success(result.message);
        return;
      }

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
      title="Renomear cadeira"
      description="MAC, endereço, firmware e ponto de acesso são cadastrados pela Physical."
      footer={
        <>
          <Button variant="ghost" size="sm" onClick={close} disabled={pending}>
            Cancelar
          </Button>
          <Button
            type="submit"
            form="rename-chair-form"
            size="sm"
            disabled={pending}
            trailingIcon={
              pending ? <Icon name="loader" className="h-4 w-4 animate-spin" /> : undefined
            }
          >
            {pending ? "Salvando..." : "Salvar"}
          </Button>
        </>
      }
    >
      {chair && (
        <form
          key={chair.id}
          id="rename-chair-form"
          onSubmit={handleSubmit}
          className="flex flex-col gap-5"
          noValidate
        >
          <input type="hidden" name="id" value={chair.id} />

          <Input
            name="name"
            label="Nome da cadeira"
            placeholder="Ex.: Sala de descanso 1"
            autoComplete="off"
            autoFocus
            maxLength={100}
            disabled={pending}
            defaultValue={chair.name}
            error={fieldErrors.name}
            leadingIcon={<Icon name="chair" />}
          />
        </form>
      )}
    </Modal>
  );
}
