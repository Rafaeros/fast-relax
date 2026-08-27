import { AppSidebar } from "@/components/layout/AppSidebar";
import type { PanelNavItem } from "@/config/navigation";

export type PanelSidebarProps = {
  /** Já filtrado pelo papel de quem está logado — ver `panelNavFor`. */
  items: PanelNavItem[];
  /** Primeiro destino do papel: o SYSADMIN não tem visão geral. */
  homeHref: string;
};

/**
 * Navegação do painel em telas grandes.
 *
 * <p>
 * Abaixo de `lg` ela some — quem navega ali é a {@link PanelTabBar}, no rodapé.
 *
 * <p>
 * Os itens chegam por prop em vez de vir do módulo de configuração: depois que a
 * plataforma passou a ter dois planos, a lista depende de quem está logado, e
 * essa informação só existe no layout, que roda no servidor.
 */
export function PanelSidebar({ items, homeHref }: PanelSidebarProps) {
  return (
    <AppSidebar items={items} homeHref={homeHref} storageKey="physical.sidebar.collapsed" />
  );
}
