"use client";

import { useState, useTransition, type FormEvent } from "react";
import { Button, Icon, Modal, useToast } from "@/components/ui";
import { CredentialDeliveryPanel } from "@/features/authentication/components/CredentialDeliveryPanel";
import { createUserAction } from "@/features/users/actions/user.actions";
import { UserFormFields } from "@/features/users/components/UserFormFields";
import { USER_INITIAL_STATE } from "@/features/users/types/user.types";
import type { CompanyOption } from "@/features/users/types/user.types";
import type { UserRole } from "@/features/authentication/types/auth.types";
import { hasFieldErrors } from "@/lib/forms";

export type CreateUserModalProps = {
  /** Disparado após o cadastro dar certo — a tabela recarrega a partir daqui. */
  onCreated: () => void;
  /** Papel de quem está cadastrando: define os perfis oferecidos. */
  currentRole: UserRole;
  /** Empresas para o SYSADMIN escolher; vazio para os demais papéis. */
  companies?: CompanyOption[];
};

export function CreateUserModal({ onCreated, currentRole, companies }: CreateUserModalProps) {
  const [open, setOpen] = useState(false);
  const [state, setState] = useState(USER_INITIAL_STATE);
  const [pending, startTransition] = useTransition();
  // Trocar a key remonta o formulário e zera os campos para o próximo cadastro.
  const [formKey, setFormKey] = useState(0);
  const toast = useToast();

  const fieldErrors = state.fieldErrors ?? {};
  // O modal não fecha sozinho no sucesso: quando sai senha temporária, ela
  // aparece uma única vez e some para sempre se ninguém copiar antes.
  const delivery = state.status === "success" ? state.credential : undefined;

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
    if (delivery) onCreated();
    resetForm();
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);

    startTransition(async () => {
      const result = await createUserAction(state, formData);

      // Aqui o modal permanece aberto no sucesso (a senha temporária precisa ser
      // copiada), então só o erro geral vira toast.
      if (result.status === "error" && result.message && !hasFieldErrors(result.fieldErrors)) {
        toast.error(result.message);
      }

      setState(result);
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
        title={delivery ? "Usuário cadastrado" : "Novo usuário"}
        description={
          delivery
            ? "Veja abaixo como o acesso foi entregue."
            : "O convite chega por e-mail; a pessoa define a própria senha pelo link."
        }
        footer={
          delivery ? (
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
        {delivery ? (
          <CredentialDeliveryPanel
            delivery={delivery}
            hint="Ele entra no painel com o e-mail cadastrado."
          />
        ) : (
          <form
            key={formKey}
            id="create-user-form"
            onSubmit={handleSubmit}
            className="flex flex-col gap-5"
            noValidate
          >
            <UserFormFields
              fieldErrors={fieldErrors}
              disabled={pending}
              currentRole={currentRole}
              companies={companies}
            />
          </form>
        )}
      </Modal>
    </>
  );
}
