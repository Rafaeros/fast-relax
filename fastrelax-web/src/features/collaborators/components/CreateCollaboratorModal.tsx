"use client";

import { useState, useTransition, type FormEvent } from "react";
import { Alert, Button, Icon, Modal, useToast } from "@/components/ui";
import { createCollaboratorAction } from "@/features/collaborators/actions/collaborator.actions";
import { AllowedWindowsField } from "@/features/collaborators/components/AllowedWindowsField";
import { CollaboratorFormFields } from "@/features/collaborators/components/CollaboratorFormFields";
import { CREATE_COLLABORATOR_INITIAL_STATE } from "@/features/collaborators/types/collaborator.types";
import type { Department } from "@/features/departments/types/department.types";

export type CreateCollaboratorModalProps = {
  departments: Department[];
  /** Disparado após o cadastro dar certo — a tabela recarrega a partir daqui. */
  onCreated: () => void;
};

export function CreateCollaboratorModal({
  departments,
  onCreated,
}: CreateCollaboratorModalProps) {
  const [open, setOpen] = useState(false);
  const [state, setState] = useState(CREATE_COLLABORATOR_INITIAL_STATE);
  const [pending, startTransition] = useTransition();
  // Trocar a key remonta o formulário: campos com estado próprio (as máscaras
  // de CPF e telefone) voltam a zero, o que `form.reset()` não faria.
  const [formKey, setFormKey] = useState(0);
  const toast = useToast();

  const fieldErrors = state.fieldErrors ?? {};
  const noDepartments = departments.length === 0;

  const resetForm = () => {
    setState(CREATE_COLLABORATOR_INITIAL_STATE);
    setFormKey((current) => current + 1);
  };

  const openModal = () => {
    resetForm();
    setOpen(true);
  };

  /**
   * O submit chama a Server Action direto (em vez de `useActionState`) porque
   * o sucesso precisa fechar o modal e avisar a tabela — decisões que dependem
   * do retorno, e que via `action=` só dariam para tratar dentro de um efeito.
   */
  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);

    startTransition(async () => {
      const result = await createCollaboratorAction(state, formData);

      if (result.status === "success") {
        resetForm();
        setOpen(false);
        onCreated();
        if (result.message) toast.success(result.message);
        return;
      }

      // Erro de campo fica no formulário, ao lado do input a corrigir. O recado
      // geral do servidor vai para o toast, que não depende do modal aberto.
      if (result.message && !result.fieldErrors) toast.error(result.message);
      setState(result);
    });
  };

  return (
    <>
      <Button
        size="sm"
        leadingIcon={<Icon name="users" className="h-4 w-4" />}
        onClick={openModal}
      >
        Novo colaborador
      </Button>

      <Modal
        open={open}
        onClose={() => setOpen(false)}
        dismissible={!pending}
        size="lg"
        title="Novo colaborador"
        description="O CPF é a credencial de acesso ao aplicativo — não pode se repetir."
        // O form fica fora do rodapé: o submit vem pelo atributo `form`.
        footer={
          <>
            <Button variant="ghost" size="sm" onClick={() => setOpen(false)} disabled={pending}>
              Cancelar
            </Button>
            <Button
              type="submit"
              form="create-collaborator-form"
              size="sm"
              disabled={pending || noDepartments}
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
          id="create-collaborator-form"
          onSubmit={handleSubmit}
          className="flex flex-col gap-5"
          noValidate
        >
          {noDepartments && (
            <Alert tone="warning" title="Nenhum departamento cadastrado">
              Cadastre um departamento antes de incluir colaboradores.
            </Alert>
          )}

          <CollaboratorFormFields
            departments={departments}
            fieldErrors={fieldErrors}
            disabled={pending}
          />

          <AllowedWindowsField disabled={pending} error={fieldErrors.allowedWindows} />
        </form>
      </Modal>
    </>
  );
}
