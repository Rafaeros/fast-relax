import type { Route } from "next";
import type { IconName } from "@/components/ui/Icon";
import type { AuthUser, UserRole } from "@/features/authentication/types/auth.types";

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
   * Papéis que enxergam o item. Ausente significa "todos os autenticados".
   *
   * Depois que a plataforma passou a ter dois planos — a equipe da Physical e
   * as empresas clientes —, uma sidebar única mostraria a metade errada para
   * cada um. Declarar o alcance aqui mantém isso em um lugar só; a barreira de
   * verdade continua no backend.
   */
  roles?: UserRole[];
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
 * Quatro destinos, todos primários: é o limite que a barra de abas do celular
 * comporta sem menu "Mais", e na sidebar do desktop dispensa agrupamento por
 * seção.
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
    label: "Notificações",
    description: "Avisos sobre suas massagens.",
    href: "/colaborador/notificacoes" as Route,
    mobilePrimary: true,
    icon: "bell",
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
  "/painel/empresas": "Empresas",
  "/painel/firmwares": "Firmware",
};

const COMPANY_ROLES: UserRole[] = ["COMPANY_ADMIN", "COMPANY_RH"];

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
    roles: COMPANY_ROLES,
    mobilePrimary: true,
    icon: "dashboard",
  },
  {
    section: "Plataforma",
    label: "Empresas",
    description: "Clientes atendidos, com endereço e situação do contrato.",
    href: "/painel/empresas" as Route,
    roles: ["SYSADMIN"],
    mobilePrimary: true,
    icon: "building",
  },
  {
    section: "Plataforma",
    label: "Firmwares",
    description: "Versões publicadas para os dispositivos ESP32.",
    href: "/painel/firmwares" as Route,
    roles: ["SYSADMIN"],
    mobilePrimary: true,
    icon: "chair",
  },
  {
    section: "Operação",
    label: "Agenda",
    description: "Sessões dos colaboradores por mês.",
    href: "/painel/agenda" as Route,
    roles: COMPANY_ROLES,
    mobilePrimary: true,
    icon: "calendar",
  },
  {
    section: "Acessos",
    label: "Usuários",
    description: "Contas com acesso ao painel.",
    href: "/painel/usuarios" as Route,
    roles: ["SYSADMIN", "COMPANY_ADMIN"],
    mobilePrimary: true,
    icon: "shield",
  },
  {
    section: "Cadastros",
    label: "Colaboradores",
    description: "Quem pode agendar e usar as cadeiras de massagem.",
    href: "/painel/colaboradores" as Route,
    roles: COMPANY_ROLES,
    mobilePrimary: true,
    icon: "users",
  },
  {
    section: "Cadastros",
    label: "Departamentos",
    description: "Áreas usadas para agrupar colaboradores nos indicadores.",
    href: "/painel/departamentos" as Route,
    roles: COMPANY_ROLES,
    icon: "building",
  },
  {
    section: "Cadastros",
    label: "Cadeiras",
    description: "Dispositivos ESP32 que acionam as cadeiras de massagem.",
    href: "/painel/cadeiras" as Route,
    // A equipe da plataforma também: é ela que instala o equipamento e grava a
    // rede do cliente nele. Cadeira é ativo da Physical, não dado pessoal.
    roles: ["SYSADMIN", ...COMPANY_ROLES],
    mobilePrimary: true,
    icon: "chair",
  },
  {
    section: "Configurações",
    label: "Configuração da sessão",
    description: "Duração, tolerância de início e antecedência de reserva.",
    href: "/painel/configuracao-sessao" as Route,
    roles: COMPANY_ROLES,
    icon: "clock",
  },
];

/** O que este papel enxerga na sidebar e na barra de abas. */
export function panelNavFor(user: Pick<AuthUser, "role"> | null | undefined): PanelNavItem[] {
  if (!user) return [];
  return PANEL_NAV.filter((item) => !item.roles || item.roles.includes(user.role));
}

/**
 * Primeiro destino do papel, para onde mandar quem chega em `/painel` sem ter
 * uma visão geral. O SYSADMIN não tem: o painel de indicadores é de uma empresa.
 */
export function panelHomeFor(user: Pick<AuthUser, "role"> | null | undefined): Route {
  const [first] = panelNavFor(user);
  return (first?.href ?? "/painel") as Route;
}

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
