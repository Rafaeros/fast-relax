"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { Icon } from "@/components/ui";
import { MOBILE_TAB_LABELS, type PanelNavItem } from "@/config/navigation";
import { cn } from "@/lib/cn";


function isActive(pathname: string, href: string, homeHref: string): boolean {
  return href === homeHref ? pathname === href : pathname.startsWith(href);
}

/** Rótulo curto na aba; o nome completo continua no menu "Mais". */
function tabLabel(item: PanelNavItem): string {
  return MOBILE_TAB_LABELS[item.href] ?? item.label;
}

/**
 * Barra de abas fixa no rodapé, no lugar da sidebar em telas pequenas.
 *
 * <p>
 * Só aparece abaixo de `lg`. O padding inferior acompanha
 * `env(safe-area-inset-bottom)` para as abas não ficarem sob a barra de gestos
 * quando o app roda empacotado pelo Capacitor.
 */
export type PanelTabBarProps = {
  /** Ja filtrado pelo papel de quem esta logado — ver `panelNavFor`. */
  items: PanelNavItem[];
  /** Rota do primeiro destino do papel; so ela exige correspondencia exata. */
  homeHref: string;
};

export function PanelTabBar({ items, homeHref }: PanelTabBarProps) {
  const pathname = usePathname();
  const primaryItems = items.filter((item) => item.mobilePrimary);
  const secondaryItems = items.filter((item) => !item.mobilePrimary);
  // A rota em que a folha foi aberta viaja junto com o estado: quando o
  // pathname muda, `openedAt` deixa de bater e ela fecha sozinha, sem efeito
  // sincronizando estado.
  const [menu, setMenu] = useState({ open: false, openedAt: pathname });

  const menuOpen = menu.open && menu.openedAt === pathname;
  const toggleMenu = () => setMenu({ open: !menuOpen, openedAt: pathname });
  const closeMenu = () => setMenu({ open: false, openedAt: pathname });

  // A rolagem de fundo precisa parar enquanto a folha cobre a tela, senão o
  // conteúdo desliza atrás do overlay no toque.
  useEffect(() => {
    if (!menuOpen) return;
    const previous = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = previous;
    };
  }, [menuOpen]);

  const secondaryActive = secondaryItems.some((item) => isActive(pathname, item.href, homeHref));

  return (
    <>
      {menuOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/60 backdrop-blur-sm lg:hidden"
          onClick={closeMenu}
          aria-hidden
        />
      )}

      {/* Folha inferior com o que não coube nas abas. */}
      <div
        id="panel-more-menu"
        role="dialog"
        aria-modal={menuOpen}
        aria-label="Mais seções"
        className={cn(
          "fixed inset-x-0 bottom-0 z-50 flex flex-col lg:hidden",
          "rounded-t-3xl border-t border-line bg-surface-nav",
          "transition-transform duration-250 ease-out",
          menuOpen ? "translate-y-0" : "pointer-events-none translate-y-full",
        )}
        style={{
          // A barra de abas fica por cima da folha (mesmo z, depois no DOM), então
          // o conteúdo precisa terminar acima dela — sem isto o último item some.
          paddingBottom: "calc(3.5rem + env(safe-area-inset-bottom))",
          // Teto para a folha não cobrir a tela inteira quando houver muitos itens.
          maxHeight: "75dvh",
        }}
      >
        {/* Alça: sinaliza que a folha é arrastável/dispensável. */}
        <div className="flex shrink-0 justify-center pt-3 pb-1">
          <div className="h-1 w-10 rounded-full bg-line" />
        </div>

        <ul className="flex min-h-0 flex-col gap-1 overflow-y-auto p-3">
          {secondaryItems.map((item) => {
            const active = isActive(pathname, item.href, homeHref);
            return (
              <li key={item.href}>
                <Link
                  href={item.href}
                  aria-current={active ? "page" : undefined}
                  className={cn(
                    "flex items-center gap-3 rounded-control px-3 py-3.5",
                    "focus-visible:outline-none focus-visible:shadow-focus",
                    active ? "bg-surface-hover text-ink-primary" : "text-ink-secondary",
                  )}
                >
                  <Icon
                    name={item.icon}
                    className={cn("h-5 w-5 shrink-0", active ? "text-accent-soft" : "text-ink-muted")}
                  />
                  <span className="flex min-w-0 flex-col">
                    <span className="text-sm font-medium">{item.label}</span>
                    <span className="truncate text-xs text-ink-muted">{item.description}</span>
                  </span>
                </Link>
              </li>
            );
          })}
        </ul>
      </div>

      <nav
        aria-label="Navegação principal"
        className={cn(
          "fixed inset-x-0 bottom-0 z-50 border-t border-line bg-surface-nav lg:hidden",
        )}
        style={{ paddingBottom: "env(safe-area-inset-bottom)" }}
      >
        <ul className="flex items-stretch">
          {primaryItems.map((item) => {
            const active = isActive(pathname, item.href, homeHref) && !menuOpen;
            return (
              <li key={item.href} className="flex-1">
                <Link
                  href={item.href}
                  aria-current={active ? "page" : undefined}
                  // min-h-14: alvo de toque confortável mesmo com o rótulo pequeno.
                  className={cn(
                    "flex min-h-14 flex-col items-center justify-center gap-1 px-1 py-2",
                    "focus-visible:outline-none focus-visible:shadow-focus",
                    active ? "text-accent-soft" : "text-ink-muted",
                  )}
                >
                  <Icon name={item.icon} className="h-5 w-5 shrink-0" />
                  <span className="text-[0.6875rem] leading-none font-medium">
                    {tabLabel(item)}
                  </span>
                </Link>
              </li>
            );
          })}

          <li className="flex-1">
            <button
              type="button"
              onClick={toggleMenu}
              aria-expanded={menuOpen}
              aria-controls="panel-more-menu"
              className={cn(
                "flex min-h-14 w-full flex-col items-center justify-center gap-1 px-1 py-2",
                "focus-visible:outline-none focus-visible:shadow-focus",
                menuOpen || secondaryActive ? "text-accent-soft" : "text-ink-muted",
              )}
            >
              <Icon name={menuOpen ? "close" : "menu"} className="h-5 w-5 shrink-0" />
              <span className="text-[0.6875rem] leading-none font-medium">Mais</span>
            </button>
          </li>
        </ul>
      </nav>
    </>
  );
}
