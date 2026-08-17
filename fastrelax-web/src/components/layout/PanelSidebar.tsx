import { AppSidebar } from "@/components/layout/AppSidebar";
import { PANEL_NAV } from "@/config/navigation";

/**
 * Navegação do painel do RH em telas grandes.
 *
 * <p>
 * Abaixo de `lg` ela some — quem navega ali é a {@link PanelTabBar}, no rodapé.
 */
export function PanelSidebar() {
  return (
    <AppSidebar items={PANEL_NAV} homeHref="/painel" storageKey="physical.sidebar.collapsed" />
  );
}
