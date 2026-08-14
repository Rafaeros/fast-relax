"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useSyncExternalStore } from "react";
import { Icon, IconButton, Logo } from "@/components/ui";
import { PANEL_NAV } from "@/config/navigation";
import { cn } from "@/lib/cn";

const STORAGE_KEY = "physical.sidebar.collapsed";

/**
 * A preferência mora no localStorage — um sistema externo ao React. Tratá-la
 * como store (em vez de estado sincronizado por efeito) evita render em cascata
 * e deixa o React resolver a hidratação: servidor sempre renderiza expandido.
 */
const listeners = new Set<() => void>();
let snapshot: boolean | null = null;

function subscribe(onStoreChange: () => void): () => void {
  listeners.add(onStoreChange);
  return () => listeners.delete(onStoreChange);
}

function getSnapshot(): boolean {
  if (snapshot === null) {
    snapshot = window.localStorage.getItem(STORAGE_KEY) === "true";
  }
  return snapshot;
}

/** No servidor não há preferência: a coluna nasce expandida. */
function getServerSnapshot(): boolean {
  return false;
}

function setCollapsed(value: boolean): void {
  snapshot = value;
  window.localStorage.setItem(STORAGE_KEY, String(value));
  listeners.forEach((listener) => listener());
}

/**
 * Navegação do painel. Em telas grandes é uma coluna de altura total, do topo
 * à base, que pode ser recolhida para só a marca e os ícones. Abaixo de `lg`
 * vira uma faixa horizontal rolável, sem menu recolhível.
 */
export function PanelSidebar() {
  const pathname = usePathname();
  const collapsed = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);

  const toggleCollapsed = () => setCollapsed(!collapsed);

  return (
    <nav
      aria-label="Seções do painel"
      data-collapsed={collapsed}
      className={cn(
        "flex shrink-0 border-line bg-surface-nav",
        "max-lg:overflow-x-auto max-lg:border-b",
        "lg:h-full lg:flex-col lg:border-r",
        "lg:transition-[width] lg:duration-200 lg:ease-out",
        collapsed ? "lg:w-[4.5rem]" : "lg:w-64",
      )}
    >
      {/* Marca no topo da coluna: no mobile ela fica na topbar. */}
      <div
        className={cn(
          "hidden h-16 shrink-0 items-center border-b border-line lg:flex",
          collapsed ? "justify-center px-2" : "px-4",
        )}
      >
        <Link href="/painel" aria-label="Ir para a visão geral">
          <Logo height={24} variant={collapsed ? "mark" : "full"} />
        </Link>
      </div>

      <ul
        className={cn(
          "flex flex-1 gap-1 overflow-y-auto p-3 max-lg:min-w-max",
          "lg:flex-col lg:gap-1.5",
          collapsed && "lg:items-center lg:p-2",
        )}
      >
        {PANEL_NAV.map((item) => {
          const active =
            item.href === "/painel" ? pathname === item.href : pathname.startsWith(item.href);

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
                  className={cn(
                    "text-sm font-medium whitespace-nowrap",
                    collapsed && "lg:hidden",
                  )}
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
          "hidden shrink-0 border-t border-line p-3 lg:flex",
          collapsed ? "justify-center" : "justify-end",
        )}
      >
        <IconButton
          label={collapsed ? "Expandir menu" : "Recolher menu"}
          aria-expanded={!collapsed}
          onClick={toggleCollapsed}
          icon={
            <Icon name={collapsed ? "chevronRight" : "chevronLeft"} className="h-4 w-4" />
          }
        />
      </div>
    </nav>
  );
}
