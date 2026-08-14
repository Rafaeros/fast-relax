"use client";

import { useEffect, useRef, type ReactNode } from "react";
import { Button } from "@/components/ui/Button";
import { Icon } from "@/components/ui/Icon";
import type { IconName } from "@/components/ui/Icon";
import { cn } from "@/lib/cn";

export type ConfirmDialogProps = {
  open: boolean;
  title: string;
  description?: ReactNode;
  confirmLabel?: string;
  cancelLabel?: string;
  /** `danger` para ações destrutivas (exclusão). */
  tone?: "danger" | "default";
  icon?: IconName;
  pending?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
};

/**
 * Confirmação bloqueante sobre `<dialog>` nativo: ESC, foco preso e backdrop
 * vêm do browser, sem biblioteca de modal nem armadilhas de acessibilidade.
 */
export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel = "Confirmar",
  cancelLabel = "Cancelar",
  tone = "default",
  icon,
  pending = false,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;

    if (open && !dialog.open) {
      dialog.showModal();
    } else if (!open && dialog.open) {
      dialog.close();
    }
  }, [open]);

  return (
    <dialog
      ref={dialogRef}
      aria-labelledby="confirm-dialog-title"
      onCancel={(event) => {
        // ESC: cancela pelo callback para o estado do pai acompanhar.
        event.preventDefault();
        if (!pending) onCancel();
      }}
      className={cn(
        "m-auto w-[min(28rem,calc(100vw-2rem))] rounded-card p-0",
        "bg-surface-card text-ink-primary shadow-card-hover",
        "border border-line backdrop:bg-bg-950/70 backdrop:backdrop-blur-sm",
      )}
    >
      <div className="flex flex-col gap-5 p-6">
        <div className="flex items-start gap-3">
          {icon && (
            <span
              className={cn(
                "grid h-10 w-10 shrink-0 place-items-center rounded-control border",
                tone === "danger"
                  ? "border-error-700 bg-error-bg text-error-400"
                  : "border-line bg-bg-900 text-accent-soft",
              )}
            >
              <Icon name={icon} className="h-5 w-5" />
            </span>
          )}
          <div className="flex flex-col gap-1.5">
            <h2 id="confirm-dialog-title" className="text-base font-semibold text-ink-primary">
              {title}
            </h2>
            {description && (
              <div className="text-sm leading-relaxed text-ink-secondary">{description}</div>
            )}
          </div>
        </div>

        <div className="flex justify-end gap-2">
          <Button variant="ghost" size="sm" onClick={onCancel} disabled={pending}>
            {cancelLabel}
          </Button>
          <Button
            size="sm"
            onClick={onConfirm}
            disabled={pending}
            className={cn(
              tone === "danger" &&
                "bg-error-600 text-neutral-50 hover:bg-error-700 hover:shadow-none active:bg-error-700",
            )}
            trailingIcon={pending ? <Icon name="loader" className="h-4 w-4 animate-spin" /> : undefined}
          >
            {confirmLabel}
          </Button>
        </div>
      </div>
    </dialog>
  );
}
