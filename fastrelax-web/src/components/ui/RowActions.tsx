import type { Route } from "next";
import Link from "next/link";
import type { MouseEventHandler } from "react";
import { cn } from "@/lib/cn";
import { Icon } from "@/components/ui/Icon";
import type { IconName } from "@/components/ui/Icon";
import { IconButton, iconButtonStyles } from "@/components/ui/IconButton";
import type { IconButtonTone } from "@/components/ui/IconButton";

export type RowActionProps = {
  /** Rótulo acessível — vira `aria-label` e tooltip. */
  label: string;
  icon: IconName;
  tone?: IconButtonTone;
  /** Navegação (ex.: abrir detalhe). Tem precedência sobre `onClick`. */
  href?: Route;
  /** Só funciona quando o componente pai é client. */
  onClick?: MouseEventHandler<HTMLButtonElement>;
  disabled?: boolean;
};

/**
 * Ação individual de linha. Vira link quando recebe `href` e botão quando
 * recebe `onClick` — sem `<button>` dentro de `<a>` em nenhum dos casos.
 */
export function RowAction({
  label,
  icon,
  tone = "neutral",
  href,
  onClick,
  disabled,
}: RowActionProps) {
  if (href && !disabled) {
    return (
      <Link
        href={href}
        aria-label={label}
        title={label}
        className={iconButtonStyles({ tone })}
      >
        <Icon name={icon} className="h-4 w-4" />
      </Link>
    );
  }

  return (
    <IconButton
      label={label}
      tone={tone}
      onClick={onClick}
      // Sem destino e sem handler a ação seria um clique morto.
      disabled={disabled || (!href && !onClick)}
      icon={<Icon name={icon} className="h-4 w-4" />}
    />
  );
}

/** Atalho: ação "ver detalhes". */
export function ViewAction(props: Omit<RowActionProps, "icon" | "label"> & { label?: string }) {
  return <RowAction icon="eye" label={props.label ?? "Ver detalhes"} {...props} />;
}

/** Atalho: ação "editar". */
export function EditAction(props: Omit<RowActionProps, "icon" | "label"> & { label?: string }) {
  return <RowAction icon="pencil" label={props.label ?? "Editar"} {...props} />;
}

export type RowActionsProps = {
  children: React.ReactNode;
  className?: string;
};

/** Agrupa as ações no fim da linha da tabela. */
export function RowActions({ children, className }: RowActionsProps) {
  return (
    <div className={cn("flex items-center justify-end gap-1", className)}>{children}</div>
  );
}
