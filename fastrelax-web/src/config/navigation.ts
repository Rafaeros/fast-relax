import type { Route } from "next";
import type { IconName } from "@/components/ui/Icon";

/**
 * Fonte única da navegação do painel.
 * Sidebar e breadcrumb da topbar leem daqui — adicionar uma seção é adicionar
 * um item nesta lista, sem tocar em componente nenhum.
 */
export type PanelNavItem = {
  /** Agrupamento exibido antes do título no breadcrumb (ex.: "Acessos"). */
  section: string;
  label: string;
  description: string;
  href: Route;
  icon: IconName;
  /**
   * Aparece direto na barra de abas do mobile. Os demais ficam no menu "Mais".
   *
   * Uma barra de abas comporta de quatro a cinco destinos antes dos alvos de
   * toque ficarem pequenos demais — daí a separação em vez de espremer tudo.
   */
  mobilePrimary?: boolean;
};

/**
 * Navegação do app do colaborador.
 *
 * <p>
 * Três destinos, todos primários: cabem na barra de abas do celular sem menu
 * "Mais", e na sidebar do desktop sem agrupamento por seção.
 */
export const COLLABORATOR_NAV: PanelNavItem[] = [
  {
    section: "App",
    label: "Início",
    description: "Sua próxima massagem e seu histórico.",
    href: "/colaborador" as Route,
    mobilePrimary: true,
    icon: "dashboard",
  },
  {
    section: "App",
    label: "Agendar",
    description: "Escolha um horário livre na sua janela.",
    href: "/colaborador/agenda" as Route,
    mobilePrimary: true,
    icon: "calendar",
  },
  {
    section: "App",
    label: "Perfil",
    description: "Seus dados e horários permitidos.",
    href: "/colaborador/perfil" as Route,
    mobilePrimary: true,
    icon: "users",
  },
];

/** Rótulo curto para a barra de abas, onde o espaço é de um ícone. */
export const MOBILE_TAB_LABELS: Partial<Record<string, string>> = {
  "/painel": "Início",
  "/painel/agenda": "Agenda",
  "/painel/colaboradores": "Equipe",
  "/painel/cadeiras": "Cadeiras",
};

/**
 * A ordem aqui é a da sidebar. Rotas mais específicas antes das mais curtas
 * não importa: a correspondência por prefixo só é usada nas subrotas de cada
 * item, e nenhum href é prefixo de outro.
 */

export const PANEL_NAV: PanelNavItem[] = [
  {
    section: "Painel",
    label: "Visão geral",
    description: "Indicadores, agenda do dia e status das cadeiras.",
    href: "/painel" as Route,
    mobilePrimary: true,
    icon: "dashboard",
  },
  {
    section: "Operação",
    label: "Agenda",
    description: "Sessões dos colaboradores por mês.",
    href: "/painel/agenda" as Route,
    mobilePrimary: true,
    icon: "calendar",
  },
  {
    section: "Acessos",
    label: "Usuários",
    description: "Contas de RH e administradores com acesso ao painel.",
    href: "/painel/usuarios" as Route,
    icon: "shield",
  },
  {
    section: "Cadastros",
    label: "Colaboradores",
    description: "Quem pode agendar e usar as cadeiras de massagem.",
    href: "/painel/colaboradores" as Route,
    mobilePrimary: true,
    icon: "users",
  },
  {
    section: "Cadastros",
    label: "Departamentos",
    description: "Áreas usadas para agrupar colaboradores nos indicadores.",
    href: "/painel/departamentos" as Route,
    icon: "building",
  },
  {
    section: "Cadastros",
    label: "Cadeiras",
    description: "Dispositivos ESP32 que acionam as cadeiras de massagem.",
    href: "/painel/cadeiras" as Route,
    mobilePrimary: true,
    icon: "chair",
  },
  {
    section: "Configurações",
    label: "Configuração da sessão",
    description: "Duração, tolerância de início e antecedência de reserva.",
    href: "/painel/configuracao-sessao" as Route,
    icon: "clock",
  },
];

/**
 * Item correspondente à rota atual.
 * "/painel" exige correspondência exata; as demais aceitam subrotas
 * (ex.: /painel/usuarios/12 continua em "Usuários").
 */
export function findPanelNavItem(pathname: string): PanelNavItem | undefined {
  return PANEL_NAV.find((item) =>
    item.href === "/painel" ? pathname === item.href : pathname.startsWith(item.href),
  );
}
