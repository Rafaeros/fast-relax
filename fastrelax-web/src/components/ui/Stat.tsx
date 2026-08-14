import type { ReactNode } from "react";
import { cn } from "@/lib/cn";

export type StatProps = {
  value: string;
  label: string;
  icon?: ReactNode;
  className?: string;
};

/** Molécula: número em destaque + rótulo (faixa de métricas do hero). */
export function Stat({ value, label, icon, className }: StatProps) {
  return (
    <div className={cn("flex flex-col gap-1.5", className)}>
      <span className="flex items-center gap-2 text-[0.6875rem] font-semibold uppercase tracking-[0.18em] text-ink-muted">
        {icon && <span className="text-accent-soft [&>svg]:h-4 [&>svg]:w-4">{icon}</span>}
        {label}
      </span>
      <span className="font-display text-2xl text-ink-primary sm:text-3xl">{value}</span>
    </div>
  );
}

export type StatGroupProps = {
  items: StatProps[];
  className?: string;
};

export function StatGroup({ items, className }: StatGroupProps) {
  return (
    <dl className={cn("flex flex-wrap gap-x-10 gap-y-6", className)}>
      {items.map((item) => (
        <Stat key={item.label} {...item} />
      ))}
    </dl>
  );
}

/** Molécula: linha "ícone + rótulo + valor" usada dentro de cards. */
export function SpecItem({
  icon,
  label,
  value,
  className,
}: {
  icon?: ReactNode;
  label: string;
  value?: string;
  className?: string;
}) {
  return (
    <span className={cn("flex items-center gap-2 text-xs text-ink-secondary", className)}>
      {icon && <span className="text-ink-muted [&>svg]:h-4 [&>svg]:w-4">{icon}</span>}
      <span className="truncate">{label}</span>
      {value && <span className="ml-auto font-semibold text-ink-primary">{value}</span>}
    </span>
  );
}
