"use client";

import { useState, useTransition } from "react";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { Icon } from "@/components/ui/Icon";
import { IconButton } from "@/components/ui/IconButton";

export type DeleteActionProps = {
  /** Nome do registro, exibido na confirmação. */
  itemName: string;
  /** Server Action ou handler de exclusão. Sem ele o botão fica inerte. */
  onConfirm?: () => void | Promise<void>;
  label?: string;
  /** Texto extra explicando a consequência da exclusão. */
  description?: string;
  disabled?: boolean;
};

/**
 * Exclusão em duas etapas: o clique abre a confirmação, e só ela dispara a ação.
 * Aceita Server Action direto em `onConfirm` — o `useTransition` mantém o
 * estado de pendência enquanto a action roda no servidor.
 */
export function DeleteAction({
  itemName,
  onConfirm,
  label = "Excluir",
  description,
  disabled,
}: DeleteActionProps) {
  const [open, setOpen] = useState(false);
  const [pending, startTransition] = useTransition();

  const handleConfirm = () => {
    if (!onConfirm) {
      setOpen(false);
      return;
    }

    startTransition(async () => {
      await onConfirm();
      setOpen(false);
    });
  };

  return (
    <>
      <IconButton
        label={label}
        tone="danger"
        disabled={disabled}
        onClick={() => setOpen(true)}
        icon={<Icon name="trash" className="h-4 w-4" />}
      />

      <ConfirmDialog
        open={open}
        tone="danger"
        icon="trash"
        title={`Excluir ${itemName}?`}
        description={
          description ?? "Esta ação não pode ser desfeita. O registro sai das listagens."
        }
        confirmLabel={pending ? "Excluindo..." : "Excluir"}
        pending={pending}
        onConfirm={handleConfirm}
        onCancel={() => setOpen(false)}
      />
    </>
  );
}
