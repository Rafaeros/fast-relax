import { AppSidebar } from "@/components/layout/AppSidebar";
import { COLLABORATOR_NAV } from "@/config/navigation";

/**
 * Navegação do app do colaborador no desktop — mesma coluna do painel do RH.
 *
 * <p>
 * No celular ela some e a navegação passa para a barra de abas do rodapé, que
 * é o formato que o app empacotado precisa ter.
 */
export function CollaboratorSidebar() {
  return (
    <AppSidebar
      items={COLLABORATOR_NAV}
      homeHref="/colaborador"
      storageKey="physical.collaborator-sidebar.collapsed"
    />
  );
}
