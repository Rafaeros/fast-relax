"use client";

import { useState, useTransition, type FormEvent } from "react";
import { Alert, Button, Icon, Modal } from "@/components/ui";
import { updateUserAction } from "@/features/users/actions/user.actions";
import { UserFormFields } from "@/features/users/components/UserFormFields";
import { USER_INITIAL_STATE, type User } from "@/features/users/types/user.types";

export type EditUserModalProps = {
  /** `null` mantém o modal fechado — o pai guarda a linha selecionada. */
  user: User | null;
  onClose: () => void;
  /** Disparado após salvar — a tabela recarrega a partir daqui. */
  onUpdated: () => void;
};

export function EditUserModal({ user, onClose, onUpdated }: EditUserModalProps) {
  const [state, setState] = useState(USER_INITIAL_STATE);
  const [pending, startTransition] = useTransition();

  const fieldErrors = state.fieldErrors ?? {};

  const close = () => {
    setState(USER_INITIAL_STATE);
    onClose();
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);

    startTransition(async () => {
      const result = await updateUserAction(state, formData);

      if (result.status === "success") {
        setState(USER_INITIAL_STATE);
        onClose();
        onUpdated();
        return;
      }

      setState(result);
    });
  };

  return (
    <Modal
      open={Boolean(user)}
      onClose={close}
      size="sm"
      dismissible={!pending}
      title="Editar usuário"
      description="Perfil de acesso não muda por aqui — a API atualiza só nome e e-mail."
      footer={
        <>
          <Button variant="ghost" size="sm" onClick={close} disabled={pending}>
            Cancelar
          </Button>
          <Button
            type="submit"
            form="edit-user-form"
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
      {user && (
        <form
          // A key troca junto com o registro: os campos remontam já com os
          // dados de quem foi selecionado, sem arrastar o anterior.
          key={user.id}
          id="edit-user-form"
          onSubmit={handleSubmit}
          className="flex flex-col gap-5"
          noValidate
        >
          <input type="hidden" name="id" value={user.id} />

          {state.status === "error" && state.message && (
            <Alert tone="error">{state.message}</Alert>
          )}

          <UserFormFields fieldErrors={fieldErrors} disabled={pending} user={user} />
        </form>
      )}
    </Modal>
  );
}
