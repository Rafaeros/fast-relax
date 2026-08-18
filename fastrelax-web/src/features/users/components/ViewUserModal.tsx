"use client";

import { useState, useTransition } from "react";
import { Alert, Badge, Button, CopyField, DetailList, Icon, Modal, useToast } from "@/components/ui";
import type { DetailItem } from "@/components/ui";
import {
  resetUserPasswordAction,
  toggleUserActiveAction,
} from "@/features/users/actions/user.actions";
import { USER_INITIAL_STATE, type User } from "@/features/users/types/user.types";

export type ViewUserModalProps = {
  /** `null` mantém o modal fechado — o pai guarda a linha selecionada. */
  user: User | null;
  onClose: () => void;
  /** Leva o mesmo registro para a edição, sem fechar e reabrir manualmente. */
  onEdit: (user: User) => void;
  /** Disparado quando a situação muda — a tabela recarrega a partir daqui. */
  onChanged: () => void;
};

/**
 * Detalhes do usuário e as operações que não cabem no formulário de edição:
 * situação e redefinição de senha vivem em endpoints próprios, ambos
 * exclusivos de ADMIN — a UI oferece e o backend decide (RH recebe 403 com
 * mensagem, entregue por toast).
 */
export function ViewUserModal({ user, onClose, onEdit, onChanged }: ViewUserModalProps) {
  const [state, setState] = useState(USER_INITIAL_STATE);
  const [pending, startTransition] = useTransition();
  const toast = useToast();

  const close = () => {
    setState(USER_INITIAL_STATE);
    onClose();
  };

  const toggleActive = () => {
    if (!user) return;

    startTransition(async () => {
      const result = await toggleUserActiveAction(user.id);

      if (result.ok) {
        setState(USER_INITIAL_STATE);
        onClose();
        onChanged();
        toast.success(result.message);
        return;
      }

      toast.error(result.message);
    });
  };

  const resetPassword = () => {
    if (!user) return;

    startTransition(async () => {
      const result = await resetUserPasswordAction(user.id);

      // No sucesso o estado guarda a senha temporária, que fica no modal para
      // ser copiada. A recusa (403 do RH, por exemplo) sai pelo toast.
      if (result.status === "error") {
        if (result.message) toast.error(result.message);
        return;
      }

      setState(result);
    });
  };

  const items: DetailItem[] = user
    ? [
        { label: "Nome", value: user.name },
        { label: "E-mail", value: user.email },
        { label: "Perfil", value: <Badge tone="neutral">{user.role}</Badge> },
        {
          label: "Situação",
          value: (
            <Badge tone={user.active ? "success" : "neutral"}>
              {user.active ? "Ativo" : "Inativo"}
            </Badge>
          ),
        },
        {
          label: "Senha",
          value: user.mustChangePassword ? "Temporária, troca pendente" : "Definida pelo usuário",
          full: true,
        },
      ]
    : [];

  return (
    <Modal
      open={Boolean(user)}
      onClose={close}
      dismissible={!pending}
      title="Detalhes do usuário"
      description="Acesso ao painel administrativo."
      footer={
        <>
          <Button variant="ghost" size="sm" onClick={close} disabled={pending}>
            Fechar
          </Button>
          {user && (
            <Button size="sm" onClick={() => onEdit(user)} disabled={pending}>
              Editar
            </Button>
          )}
        </>
      }
    >
      {user && (
        <div className="flex flex-col gap-6">
          <DetailList items={items} />

          {state.temporaryPassword ? (
            <div className="flex flex-col gap-3 border-t border-line pt-5">
              <Alert tone="warning" title="Senha exibida uma única vez">
                O sistema guarda apenas o hash. Perdendo este valor, resta redefinir de novo.
              </Alert>
              <CopyField label="Nova senha temporária" value={state.temporaryPassword} />
            </div>
          ) : (
            <div className="flex flex-wrap gap-2 border-t border-line pt-5">
              <Button
                variant="secondary"
                size="sm"
                onClick={resetPassword}
                disabled={pending}
                leadingIcon={<Icon name="key" className="h-4 w-4" />}
              >
                Redefinir senha
              </Button>
              <Button
                variant="secondary"
                size="sm"
                onClick={toggleActive}
                disabled={pending}
                leadingIcon={<Icon name={user.active ? "close" : "check"} className="h-4 w-4" />}
              >
                {user.active ? "Desativar acesso" : "Ativar acesso"}
              </Button>
            </div>
          )}
        </div>
      )}
    </Modal>
  );
}
