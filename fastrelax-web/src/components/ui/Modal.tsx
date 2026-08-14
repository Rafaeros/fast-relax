"use client";

import { useEffect, useRef, type ReactNode } from "react";
import { cn } from "@/lib/cn";
import { Icon } from "@/components/ui/Icon";
import { IconButton } from "@/components/ui/IconButton";

export type ModalSize = "sm" | "md" | "lg";

const SIZE: Record<ModalSize, string> = {
  sm: "w-[min(26rem,calc(100vw-2rem))]",
  md: "w-[min(34rem,calc(100vw-2rem))]",
  lg: "w-[min(46rem,calc(100vw-2rem))]",
};

export type ModalProps = {
  open: boolean;
  onClose: () => void;
  title: string;
  description?: string;
  size?: ModalSize;
  /** Rodapé fixo — normalmente os botões de ação do formulário. */
  footer?: ReactNode;
  /** Bloqueia fechar por ESC/backdrop (ex.: enquanto salva). */
  dismissible?: boolean;
  children: ReactNode;
};

/**
 * Modal genérico sobre `<dialog>` nativo: ESC, foco preso e backdrop vêm do
 * browser. O corpo rola sozinho, então formulário longo não estica a janela.
 */
export function Modal({
  open,
  onClose,
  title,
  description,
  size = "md",
  footer,
  dismissible = true,
  children,
}: ModalProps) {
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
      aria-labelledby="modal-title"
      onCancel={(event) => {
        event.preventDefault();
        if (dismissible) onClose();
      }}
      onClick={(event) => {
        // Clique no backdrop: o alvo é o próprio <dialog>, não o conteúdo.
        if (dismissible && event.target === dialogRef.current) onClose();
      }}
      className={cn(
        "m-auto max-h-[calc(100dvh-4rem)] rounded-card p-0",
        // Só o corpo rola. Sem isto o próprio dialog também ganha barra: a
        // borda de 1px faz o conteúdo passar do seu max-height por 2px.
        "overflow-hidden",
        "bg-surface-card text-ink-primary shadow-card-hover",
        "border border-line backdrop:bg-bg-950/70 backdrop:backdrop-blur-sm",
        SIZE[size],
      )}
    >
      <div className="flex max-h-[calc(100dvh-4rem-2px)] flex-col">
        <header className="flex shrink-0 items-start justify-between gap-4 border-b border-line p-5">
          <div className="flex flex-col gap-1">
            <h2 id="modal-title" className="text-base font-semibold text-ink-primary">
              {title}
            </h2>
            {description && <p className="text-xs text-ink-tertiary">{description}</p>}
          </div>

          <IconButton
            label="Fechar"
            onClick={onClose}
            disabled={!dismissible}
            icon={<Icon name="close" className="h-4 w-4" />}
          />
        </header>

        <div className="min-h-0 flex-1 overflow-y-auto p-5">{children}</div>

        {footer && (
          <footer className="flex shrink-0 items-center justify-end gap-2 border-t border-line p-5">
            {footer}
          </footer>
        )}
      </div>
    </dialog>
  );
}
