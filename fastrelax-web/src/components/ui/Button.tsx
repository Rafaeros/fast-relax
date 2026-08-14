import type { AnchorHTMLAttributes, ButtonHTMLAttributes, ReactNode } from "react";
import { cn } from "@/lib/cn";

export type ButtonVariant = "primary" | "secondary" | "ghost" | "link";
export type ButtonSize = "sm" | "md" | "lg";

const VARIANT: Record<ButtonVariant, string> = {
  primary: cn(
    "bg-accent text-ink-inverse font-bold",
    "hover:bg-accent-strong hover:shadow-glow-primary",
    "active:bg-copper-700",
  ),
  secondary: cn(
    "bg-transparent text-neutral-100 border border-line font-semibold",
    "hover:bg-surface-hover hover:border-line-soft",
  ),
  ghost: cn(
    "bg-transparent text-ink-secondary font-semibold",
    "hover:bg-surface-hover hover:text-ink-primary",
  ),
  link: cn(
    "bg-transparent text-accent-soft font-semibold px-0 py-0",
    "underline underline-offset-4 decoration-line hover:text-accent-faint hover:decoration-accent-soft",
  ),
};

const SIZE: Record<ButtonSize, string> = {
  sm: "text-xs px-3.5 py-2 gap-1.5",
  md: "text-sm px-4.5 py-2.5 gap-2",
  lg: "text-[0.9375rem] px-6 py-3.5 gap-2.5",
};

const BASE = cn(
  "inline-flex items-center justify-center whitespace-nowrap",
  "rounded-control tracking-wide",
  "transition-[background-color,box-shadow,border-color,color] duration-150 ease-out",
  "focus-visible:outline-none focus-visible:shadow-focus",
  "disabled:opacity-50 disabled:pointer-events-none aria-disabled:opacity-50",
);

type SharedProps = {
  variant?: ButtonVariant;
  size?: ButtonSize;
  /** Ícone à esquerda do rótulo. */
  leadingIcon?: ReactNode;
  /** Ícone à direita do rótulo (ex.: seta). */
  trailingIcon?: ReactNode;
  /** Ocupa 100% da largura do container. */
  fullWidth?: boolean;
};

/**
 * Classes do botão sem o elemento — para aplicar em algo que já renderiza a tag,
 * como `next/link` (evita <a> dentro de <a>).
 */
export function buttonStyles({
  variant = "primary",
  size = "md",
  fullWidth,
  className,
}: SharedProps & { className?: string } = {}): string {
  return cn(
    BASE,
    VARIANT[variant],
    variant === "link" ? SIZE[size].replace(/px-[\d.]+|py-[\d.]+/g, "") : SIZE[size],
    fullWidth && "w-full",
    className,
  );
}

export type ButtonProps = SharedProps & ButtonHTMLAttributes<HTMLButtonElement>;

export function Button({
  variant,
  size,
  leadingIcon,
  trailingIcon,
  fullWidth,
  className,
  children,
  type = "button",
  ...rest
}: ButtonProps) {
  return (
    <button
      type={type}
      className={buttonStyles({ variant, size, fullWidth, className })}
      {...rest}
    >
      {leadingIcon}
      {children}
      {trailingIcon}
    </button>
  );
}

export type ButtonLinkProps = SharedProps & AnchorHTMLAttributes<HTMLAnchorElement>;

/** Mesma aparência do Button, renderizado como <a> (use com next/link asChild-style). */
export function ButtonLink({
  variant,
  size,
  leadingIcon,
  trailingIcon,
  fullWidth,
  className,
  children,
  ...rest
}: ButtonLinkProps) {
  return (
    <a className={buttonStyles({ variant, size, fullWidth, className })} {...rest}>
      {leadingIcon}
      {children}
      {trailingIcon}
    </a>
  );
}
