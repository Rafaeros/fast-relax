import type { HTMLAttributes, ReactNode } from "react";
import { cn } from "@/lib/cn";

export type BadgeTone = "accent" | "neutral" | "success" | "warning" | "error" | "info";

const TONE: Record<BadgeTone, string> = {
  accent: "bg-copper-900/60 text-accent-soft border-accent-strong/60",
  neutral: "bg-surface-hover text-ink-secondary border-line",
  success: "badge-success",
  warning: "badge-warning",
  error: "badge-error",
  info: "badge-info",
};

export type BadgeProps = HTMLAttributes<HTMLSpanElement> & {
  tone?: BadgeTone;
  icon?: ReactNode;
};

export function Badge({ tone = "accent", icon, className, children, ...rest }: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-pill border px-3 py-1",
        "text-[0.6875rem] font-semibold tracking-wide uppercase",
        TONE[tone],
        className,
      )}
      {...rest}
    >
      {icon && <span className="[&>svg]:h-3.5 [&>svg]:w-3.5">{icon}</span>}
      {children}
    </span>
  );
}
