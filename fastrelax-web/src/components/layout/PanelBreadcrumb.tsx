"use client";

import { usePathname } from "next/navigation";
import { Icon } from "@/components/ui";
import { findPanelNavItem } from "@/config/navigation";

/**
 * Contexto da tela atual na topbar: "Seção > Página" com a descrição embaixo.
 * Substitui o cabeçalho repetido dentro de cada página — o conteúdo ganha a
 * altura de volta e a tabela aparece mais alta.
 */
export function PanelBreadcrumb() {
  const pathname = usePathname();
  const item = findPanelNavItem(pathname);

  if (!item) return null;

  return (
    <div className="flex min-w-0 flex-col">
      <nav aria-label="Você está em" className="flex items-center gap-1.5">
        <span className="text-[0.6875rem] font-semibold uppercase tracking-[0.16em] text-ink-muted">
          {item.section}
        </span>
        <Icon name="chevronRight" className="h-3 w-3 text-ink-tertiary" />
        <span className="truncate text-sm font-semibold text-ink-primary">{item.label}</span>
      </nav>
      <p className="truncate text-xs text-ink-tertiary">{item.description}</p>
    </div>
  );
}
