"use client";

import { useState, useTransition, type FormEvent } from "react";
import { Alert, Button, CopyField, Icon, Modal } from "@/components/ui";
import { createUserAction } from "@/features/users/actions/user.actions";
import { UserFormFields } from "@/features/users/components/UserFormFields";
import { USER_INITIAL_STATE } from "@/features/users/types/user.types";

export type CreateUserModalProps = {
  /** Disparado após o cadastro dar certo — a tabela recarrega a partir daqui. */
  onCreated: () => void;
};

export function CreateUserModal({ onCreated }: CreateUserModalProps) {
  const [open, setOpen] = useState(false);
  const [state, setState] = useState(USER_INITIAL_STATE);
  const [pending, startTransition] = useTransition();
  // Trocar a key remonta o formulário e zera os campos para o próximo cadastro.
  const [formKey, setFormKey] = useState(0);

  const fieldErrors = state.fieldErrors ?? {};
  // O modal não fecha sozinho no sucesso: a senha temporária aparece uma única
  // vez e some para sempre se o ADMIN não copiar antes.
  const createdPassword = state.status === "success" ? state.temporaryPassword : undefined;

  const resetForm = () => {
    setState(USER_INITIAL_STATE);
    setFormKey((current) => current + 1);
  };

  const openModal = () => {
    resetForm();
    setOpen(true);
  };

  const close = () => {
    setOpen(false);
    // A lista só recarrega ao fechar, já com o registro novo no lugar certo.
    if (createdPassword) onCreated();
    resetForm();
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);

    startTransition(async () => {
      setState(await createUserAction(state, formData));
    });
  };

  return (
    <>
      <Button
        size="sm"
        leadingIcon={<Icon name="shield" className="h-4 w-4" />}
        onClick={openModal}
      >
        Novo usuário
      </Button>

      <Modal
        open={open}
        onClose={close}
        size="sm"
        dismissible={!pending}
        title={createdPassword ? "Usuário cadastrado" : "Novo usuário"}
        description={
          createdPassword
            ? "Repasse a senha abaixo com segurança — ela não aparece de novo."
            : "O acesso é criado com senha temporária, trocada no primeiro login."
        }
        footer={
          createdPassword ? (
            <Button size="sm" onClick={close}>
              Concluir
            </Button>
          ) : (
            <>
              <Button variant="ghost" size="sm" onClick={close} disabled={pending}>
                Cancelar
              </Button>
              <Button
                type="submit"
                form="create-user-form"
                size="sm"
                disabled={pending}
                trailingIcon={
                  pending ? <Icon name="loader" className="h-4 w-4 animate-spin" /> : undefined
                }
              >
                {pending ? "Cadastrando..." : "Cadastrar"}
              </Button>
            </>
          )
        }
      >
        {createdPassword ? (
          <div className="flex flex-col gap-5">
            <Alert tone="warning" title="Senha exibida uma única vez">
              O sistema guarda apenas o hash. Perdendo este valor, resta redefinir a senha.
            </Alert>
            <CopyField label="Senha temporária" value={createdPassword} />
          </div>
        ) : (
          <form
            key={formKey}
            id="create-user-form"
            onSubmit={handleSubmit}
            className="flex flex-col gap-5"
            noValidate
          >
            {state.status === "error" && state.message && (
              <Alert tone="error">{state.message}</Alert>
            )}

            <UserFormFields fieldErrors={fieldErrors} disabled={pending} />
          </form>
        )}
      </Modal>
    </>
  );
}
