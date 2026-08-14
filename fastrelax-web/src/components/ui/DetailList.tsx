import type { ReactNode } from "react";
import { cn } from "@/lib/cn";

export type DetailItem = {
  label: string;
  value: ReactNode;
  /** Ocupa a linha inteira mesmo no layout de duas colunas. */
  full?: boolean;
};

export type DetailListProps = {
  items: DetailItem[];
  columns?: 1 | 2;
  className?: string;
};

/**
 * Lista rótulo/valor para telas de visualização.
 * `<dl>` de verdade: leitor de tela anuncia o par, não dois textos soltos.
 */
export function DetailList({ items, columns = 2, className }: DetailListProps) {
  return (
    <dl
      className={cn(
        "grid gap-x-6 gap-y-5",
        columns === 2 ? "sm:grid-cols-2" : "grid-cols-1",
        className,
      )}
    >
      {items.map((item) => (
        <div
          key={item.label}
          className={cn("flex flex-col gap-1", item.full && "sm:col-span-full")}
        >
          <dt className="text-[0.6875rem] font-semibold uppercase tracking-[0.14em] text-ink-muted">
            {item.label}
          </dt>
          <dd className="text-sm text-ink-primary">{item.value}</dd>
        </div>
      ))}
    </dl>
  );
}
