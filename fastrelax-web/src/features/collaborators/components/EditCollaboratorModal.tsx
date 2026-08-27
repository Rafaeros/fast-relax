"use client";

import { useState, useTransition, type FormEvent } from "react";
import { Alert, Button, Icon, Modal, useToast } from "@/components/ui";
import { updateCollaboratorAction } from "@/features/collaborators/actions/collaborator.actions";
import { AllowedWindowsField } from "@/features/collaborators/components/AllowedWindowsField";
import { CollaboratorFormFields } from "@/features/collaborators/components/CollaboratorFormFields";
import { useCollaboratorSchedule } from "@/features/collaborators/hooks/useCollaboratorSchedule";
import {
  CREATE_COLLABORATOR_INITIAL_STATE,
  type Collaborator,
} from "@/features/collaborators/types/collaborator.types";
import type { Department } from "@/features/departments/types/department.types";
import { hasFieldErrors } from "@/lib/forms";

export type EditCollaboratorModalProps = {
  /** `null` mantém o modal fechado — o pai guarda a linha selecionada. */
  collaborator: Collaborator | null;
  departments: Department[];
  onClose: () => void;
  /** Disparado após salvar — a tabela recarrega a partir daqui. */
  onUpdated: () => void;
};

export function EditCollaboratorModal({
  collaborator,
  departments,
  onClose,
  onUpdated,
}: EditCollaboratorModalProps) {
  const [state, setState] = useState(CREATE_COLLABORATOR_INITIAL_STATE);
  const [pending, startTransition] = useTransition();
  const {
    windows,
    loading: loadingSchedule,
    error: scheduleError,
  } = useCollaboratorSchedule(collaborator?.id ?? null);
  const toast = useToast();

  const fieldErrors = state.fieldErrors ?? {};

  const close = () => {
    setState(CREATE_COLLABORATOR_INITIAL_STATE);
    onClose();
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);

    startTransition(async () => {
      const result = await updateCollaboratorAction(state, formData);

      if (result.status === "success") {
        setState(CREATE_COLLABORATOR_INITIAL_STATE);
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
      open={Boolean(collaborator)}
      onClose={close}
      dismissible={!pending}
      title="Editar colaborador"
      description="Alterar o CPF troca a credencial de login do colaborador."
      footer={
        <>
          <Button variant="ghost" size="sm" onClick={close} disabled={pending}>
            Cancelar
          </Button>
          <Button
            type="submit"
            form="edit-collaborator-form"
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
      {collaborator && (
        <form
          // A key troca com o registro e com o horário carregado: os campos
          // remontam com os dados de quem foi selecionado, e o widget de
          // horário nasce já preenchido quando a busca termina.
          key={`${collaborator.id}:${loadingSchedule ? "loading" : JSON.stringify(windows)}`}
          id="edit-collaborator-form"
          onSubmit={handleSubmit}
          className="flex flex-col gap-5"
          noValidate
        >
          <input type="hidden" name="id" value={collaborator.id} />

          <CollaboratorFormFields
            departments={departments}
            fieldErrors={fieldErrors}
            disabled={pending}
            collaborator={collaborator}
            showActive
          />

          {loadingSchedule ? (
            <span className="flex items-center gap-2 text-xs text-ink-tertiary">
              <Icon name="loader" className="h-4 w-4 animate-spin" />
              Carregando horário permitido...
            </span>
          ) : (
            <>
              {scheduleError && <Alert tone="error">{scheduleError}</Alert>}
              <AllowedWindowsField
                disabled={pending}
                defaultValue={windows}
                error={fieldErrors.allowedWindows}
              />
              <p className="text-xs text-ink-tertiary">
                Salvar substitui a semana inteira. Deixar a lista vazia mantém o horário atual —
                a API não aceita semana sem nenhum dia.
              </p>
            </>
          )}
        </form>
      )}
    </Modal>
  );
}
