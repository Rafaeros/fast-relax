"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import type { Route } from "next";
import { Icon } from "@/components/ui";
import type { IconName } from "@/components/ui/Icon";
import { cn } from "@/lib/cn";

type Tab = { href: Route; label: string; icon: IconName };

/**
 * Três destinos, o que cabe folgado numa barra de abas — cada aba fica larga e
 * o alvo de toque é generoso.
 */
const TABS: Tab[] = [
  { href: "/colaborador" as Route, label: "Início", icon: "dashboard" },
  { href: "/colaborador/agenda" as Route, label: "Agendar", icon: "calendar" },
  { href: "/colaborador/perfil" as Route, label: "Perfil", icon: "users" },
];

function isActive(pathname: string, href: string): boolean {
  return href === "/colaborador" ? pathname === href : pathname.startsWith(href);
}

/**
 * Barra de abas fixa no rodapé, para o celular.
 *
 * <p>
 * Some a partir de `lg`, onde a {@link CollaboratorSidebar} assume — no
 * desktop uma barra colada na base do monitor fica longe do olhar e do cursor.
 */
export function CollaboratorTabBar() {
  const pathname = usePathname();

  return (
    <nav
      aria-label="Navegação do app"
      className={cn(
        "fixed inset-x-0 bottom-0 z-50 border-t border-line bg-surface-nav lg:hidden",
      )}
      style={{ paddingBottom: "env(safe-area-inset-bottom)" }}
    >
      <ul className="mx-auto flex max-w-3xl items-stretch">
        {TABS.map((tab) => {
          const active = isActive(pathname, tab.href);

          return (
            <li key={tab.href} className="flex-1">
              <Link
                href={tab.href}
                aria-current={active ? "page" : undefined}
                // min-h-14: alvo de toque confortável mesmo com o rótulo pequeno.
                className={cn(
                  "flex min-h-14 flex-col items-center justify-center gap-1 px-1 py-2",
                  "focus-visible:outline-none focus-visible:shadow-focus",
                  active ? "text-accent-soft" : "text-ink-muted",
                )}
              >
                <Icon name={tab.icon} className="h-5 w-5 shrink-0" />
                <span className="text-[0.6875rem] leading-none font-medium">{tab.label}</span>
              </Link>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
