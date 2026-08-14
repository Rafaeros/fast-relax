import type { ButtonHTMLAttributes, ReactNode } from "react";
import { cn } from "@/lib/cn";

export type IconButtonTone = "neutral" | "accent" | "danger";
export type IconButtonSize = "sm" | "md";

const TONE: Record<IconButtonTone, string> = {
  neutral: "text-ink-muted hover:bg-surface-hover hover:text-ink-primary",
  accent: "text-accent-soft hover:bg-surface-hover hover:text-accent-faint",
  danger: "text-ink-muted hover:bg-error-bg hover:text-error-400",
};

const SIZE: Record<IconButtonSize, string> = {
  sm: "h-8 w-8",
  md: "h-9 w-9",
};

const BASE = cn(
  "inline-grid place-items-center rounded-control border border-transparent",
  "transition-colors duration-150 ease-out",
  "focus-visible:outline-none focus-visible:shadow-focus",
  "disabled:opacity-40 disabled:pointer-events-none",
  "aria-disabled:opacity-40 aria-disabled:pointer-events-none",
);

/**
 * Classes do botão-ícone sem o elemento — para aplicar em `next/link` e evitar
 * um `<button>` dentro de um `<a>`.
 */
export function iconButtonStyles({
  tone = "neutral",
  size = "sm",
  className,
}: {
  tone?: IconButtonTone;
  size?: IconButtonSize;
  className?: string;
} = {}): string {
  return cn(BASE, TONE[tone], SIZE[size], className);
}

export type IconButtonProps = Omit<ButtonHTMLAttributes<HTMLButtonElement>, "children"> & {
  /** Rótulo acessível — vira `aria-label` e tooltip nativo. Obrigatório: o botão não tem texto. */
  label: string;
  icon: ReactNode;
  tone?: IconButtonTone;
  size?: IconButtonSize;
};

export function IconButton({
  label,
  icon,
  tone,
  size,
  className,
  type = "button",
  ...rest
}: IconButtonProps) {
  return (
    <button
      type={type}
      aria-label={label}
      title={label}
      className={iconButtonStyles({ tone, size, className })}
      {...rest}
    >
      {icon}
    </button>
  );
}
