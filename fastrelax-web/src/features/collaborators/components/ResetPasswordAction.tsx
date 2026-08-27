"use client";

import { useState, useTransition } from "react";
import { Alert, Button, CopyField, Modal, RowAction, useToast } from "@/components/ui";
import { resetCollaboratorPasswordAction } from "@/features/collaborators/actions/collaborator.actions";
import type { Collaborator } from "@/features/collaborators/types/collaborator.types";

export type ResetPasswordActionProps = {
  collaborator: Collaborator;
};

/**
 * Redefinição de senha pelo RH.
 *
 * <p>
 * Existe porque, com o CPF deixando de ser credencial, esquecer a senha virou
 * um caminho sem saída — não há e-mail cadastrado do colaborador para um fluxo
 * de recuperação automática.
 *
 * <p>
 * Pede confirmação antes: a senha atual é invalidada na hora, e clicar por
 * engano deixaria a pessoa sem acesso até alguém repassar a nova.
 */
export function ResetPasswordAction({ collaborator }: ResetPasswordActionProps) {
  const [open, setOpen] = useState(false);
  const [temporaryPassword, setTemporaryPassword] = useState<string | null>(null);
  const [pending, startTransition] = useTransition();
  const toast = useToast();

  const close = () => {
    setOpen(false);
    setTemporaryPassword(null);
  };

  const confirm = () => {
    startTransition(async () => {
      const result = await resetCollaboratorPasswordAction(collaborator.id);

      if (!result.ok || !result.temporaryPassword) {
        toast.error(result.message);
        return;
      }

      // O modal continua aberto: a senha aparece uma única vez.
      setTemporaryPassword(result.temporaryPassword);
      toast.success(result.message);
    });
  };

  return (
    <>
      <RowAction label="Redefinir senha" icon="key" onClick={() => setOpen(true)} />

      <Modal
        open={open}
        onClose={close}
        size="sm"
        dismissible={!pending}
        title={temporaryPassword ? "Senha redefinida" : "Redefinir senha"}
        description={
          temporaryPassword
            ? "Repasse a senha abaixo com segurança — ela não aparece de novo."
            : `A senha atual de ${collaborator.name} deixa de valer imediatamente.`
        }
        footer={
          temporaryPassword ? (
            <Button size="sm" onClick={close}>
              Concluir
            </Button>
          ) : (
            <>
              <Button variant="ghost" size="sm" onClick={close} disabled={pending}>
                Cancelar
              </Button>
              <Button size="sm" onClick={confirm} disabled={pending}>
                {pending ? "Redefinindo..." : "Redefinir"}
              </Button>
            </>
          )
        }
      >
        {temporaryPassword ? (
          <div className="flex flex-col gap-5">
            <Alert tone="warning" title="Senha exibida uma única vez">
              O sistema guarda apenas o hash. Perdendo este valor, resta redefinir de novo.
            </Alert>
            <CopyField label="Senha temporária" value={temporaryPassword} />
          </div>
        ) : (
          <p className="text-sm text-ink-secondary">
            O colaborador será obrigado a escolher uma nova senha no próximo acesso, e as
            sessões abertas nos aparelhos dele são encerradas.
          </p>
        )}
      </Modal>
    </>
  );
}
