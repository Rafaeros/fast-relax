"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useSyncExternalStore } from "react";
import { Icon, IconButton, Logo } from "@/components/ui";
import type { PanelNavItem } from "@/config/navigation";
import { cn } from "@/lib/cn";

/**
 * A preferência mora no localStorage — um sistema externo ao React. Tratá-la
 * como store (em vez de estado sincronizado por efeito) evita render em cascata
 * e deixa o React resolver a hidratação: servidor sempre renderiza expandido.
 *
 * O registro é por chave porque painel e app do colaborador têm colunas
 * independentes: recolher uma não deve recolher a outra.
 */
const listeners = new Map<string, Set<() => void>>();
const snapshots = new Map<string, boolean>();

function subscribe(key: string, onStoreChange: () => void): () => void {
  const set = listeners.get(key) ?? new Set();
  set.add(onStoreChange);
  listeners.set(key, set);
  return () => set.delete(onStoreChange);
}

function getSnapshot(key: string): boolean {
  if (!snapshots.has(key)) {
    snapshots.set(key, window.localStorage.getItem(key) === "true");
  }
  return snapshots.get(key) ?? false;
}

function setCollapsed(key: string, value: boolean): void {
  snapshots.set(key, value);
  window.localStorage.setItem(key, String(value));
  listeners.get(key)?.forEach((listener) => listener());
}

export type AppSidebarProps = {
  items: PanelNavItem[];
  /** Destino do clique na marca, no topo da coluna. */
  homeHref: string;
  /** Chave do localStorage — uma por área, para as preferências não colidirem. */
  storageKey: string;
};

/**
 * Coluna de navegação de altura total, do topo à base, recolhível para só a
 * marca e os ícones.
 *
 * <p>
 * Some abaixo de `lg`: no celular quem navega é a barra de abas do rodapé, que
 * é o padrão esperado de um aplicativo.
 */
export function AppSidebar({ items, homeHref, storageKey }: AppSidebarProps) {
  const pathname = usePathname();
  const collapsed = useSyncExternalStore(
    (onStoreChange) => subscribe(storageKey, onStoreChange),
    () => getSnapshot(storageKey),
    // No servidor não há preferência: a coluna nasce expandida.
    () => false,
  );

  return (
    <nav
      aria-label="Navegação principal"
      data-collapsed={collapsed}
      className={cn(
        "hidden shrink-0 border-line bg-surface-nav",
        "lg:flex lg:h-full lg:flex-col lg:border-r",
        "lg:transition-[width] lg:duration-200 lg:ease-out",
        collapsed ? "lg:w-[4.5rem]" : "lg:w-64",
      )}
    >
      <div
        className={cn(
          "flex h-16 shrink-0 items-center border-b border-line",
          collapsed ? "justify-center px-2" : "px-4",
        )}
      >
        <Link href={homeHref} aria-label="Ir para o início">
          <Logo height={24} variant={collapsed ? "mark" : "full"} />
        </Link>
      </div>

      <ul
        className={cn(
          "flex flex-1 flex-col gap-1.5 overflow-y-auto p-3",
          collapsed && "lg:items-center lg:p-2",
        )}
      >
        {items.map((item) => {
          const active =
            item.href === homeHref ? pathname === item.href : pathname.startsWith(item.href);

          return (
            <li key={item.href} className={cn(!collapsed && "lg:w-full")}>
              <Link
                href={item.href}
                aria-current={active ? "page" : undefined}
                // Com a coluna recolhida o rótulo some: o title vira a única pista.
                title={collapsed ? item.label : undefined}
                className={cn(
                  "flex items-center gap-3 rounded-control transition-colors",
                  "focus-visible:outline-none focus-visible:shadow-focus",
                  collapsed ? "px-3 py-2.5 lg:justify-center lg:px-2.5" : "px-3 py-2.5",
                  active
                    ? "bg-surface-hover text-ink-primary"
                    : "text-ink-secondary hover:bg-surface-hover/60 hover:text-ink-primary",
                )}
              >
                <Icon
                  name={item.icon}
                  className={cn("h-5 w-5 shrink-0", active ? "text-accent-soft" : "text-ink-muted")}
                />
                <span
                  className={cn("text-sm font-medium whitespace-nowrap", collapsed && "lg:hidden")}
                >
                  {item.label}
                </span>
              </Link>
            </li>
          );
        })}
      </ul>

      <div
        className={cn(
          "flex shrink-0 border-t border-line p-3",
          collapsed ? "justify-center" : "justify-end",
        )}
      >
        <IconButton
          label={collapsed ? "Expandir menu" : "Recolher menu"}
          aria-expanded={!collapsed}
          onClick={() => setCollapsed(storageKey, !collapsed)}
          icon={<Icon name={collapsed ? "chevronRight" : "chevronLeft"} className="h-4 w-4" />}
        />
      </div>
    </nav>
  );
}
