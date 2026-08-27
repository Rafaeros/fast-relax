import type { ReactNode } from "react";
import { redirect } from "next/navigation";
import { Badge, Logo } from "@/components/ui";
import { PanelBreadcrumb } from "@/components/layout/PanelBreadcrumb";
import { PanelSidebar } from "@/components/layout/PanelSidebar";
import { PanelTabBar } from "@/components/layout/PanelTabBar";
import { panelHomeFor, panelNavFor } from "@/config/navigation";
import { LogoutButton } from "@/features/authentication/components/LogoutButton";
import { ROLE_SHORT_LABELS } from "@/features/authentication/lib/roles";
import { getCurrentUser } from "@/features/authentication/services/auth.service";

/**
 * Shell das rotas do painel. O guard fica aqui: qualquer página sob `/painel`
 * só renderiza com sessão válida — sem token, redireciona ao login.
 *
 * A altura é travada em uma tela (`h-dvh` + `overflow-hidden`): a sidebar vai
 * de topo a base, e só as áreas internas rolam. A topbar pertence à coluna de
 * conteúdo, então não passa por cima da sidebar.
 *
 * No mobile a estrutura muda para a de um aplicativo: sidebar some, o header
 * encosta no topo respeitando o notch, e a navegação vira uma barra de abas
 * fixa no rodapé. `h-dvh` (e não `h-screen`) importa aqui — no navegador do
 * celular a barra de endereço recolhe, e só a unidade dinâmica acompanha.
 */
export default async function PainelLayout({ children }: { children: ReactNode }) {
  const user = await getCurrentUser();
  if (!user) redirect("/entrar");

  // Com senha temporária o backend bloqueia todo o resto da API: qualquer tela
  // daqui renderizaria erro, então o caminho é um só.
  if (user.mustChangePassword) redirect("/definir-senha");

  // A navegação depende do papel: a equipe da Physical administra empresas e
  // firmwares, os papéis de cliente operam a própria empresa.
  const navItems = panelNavFor(user);
  const homeHref = panelHomeFor(user);

  return (
    <div className="flex h-dvh flex-col overflow-hidden lg:flex-row">
      <PanelSidebar items={navItems} homeHref={homeHref} />

      <div className="flex min-h-0 min-w-0 flex-1 flex-col">
        <header
          className="shrink-0 border-b border-line bg-surface-nav"
          // O header desenha atrás da status bar quando empacotado; o padding
          // devolve o espaço para o conteúdo não ficar sob o relógio do sistema.
          style={{ paddingTop: "env(safe-area-inset-top)" }}
        >
          <div className="flex h-14 items-center justify-between gap-3 px-4 sm:px-8 lg:h-16">
            <div className="flex min-w-0 items-center gap-3">
              {/* A logo mora na sidebar a partir de lg; aqui ela cobre o mobile. */}
              <Logo height={20} variant="mark" className="lg:hidden" />
              <PanelBreadcrumb />
            </div>

            <div className="flex shrink-0 items-center gap-2 sm:gap-3">
              <Badge tone="neutral" className="hidden sm:inline-flex">
                {ROLE_SHORT_LABELS[user.role]}
              </Badge>
              {/*
                A empresa aparece ao lado do nome porque o mesmo painel agora
                atende clientes diferentes — sem isso não há como saber, olhando
                a tela, em qual deles se está operando.
              */}
              <span className="hidden min-w-0 flex-col text-right lg:flex">
                <span className="truncate text-sm text-ink-secondary">{user.name}</span>
                {user.companyName && (
                  <span className="truncate text-xs text-ink-tertiary">{user.companyName}</span>
                )}
              </span>
              <LogoutButton />
            </div>
          </div>
        </header>

        {/*
          Rolagem da página acontece aqui dentro, nunca no body.
          O padding inferior no mobile reserva a altura da barra de abas mais a
          área segura, senão o último item da lista fica coberto por ela.
        */}
        <main className="min-h-0 flex-1 overflow-y-auto px-4 py-6 pb-[calc(4.5rem+env(safe-area-inset-bottom))] sm:px-8 lg:py-8 lg:pb-8">
          {children}
        </main>
      </div>

      <PanelTabBar items={navItems} homeHref={homeHref} />
    </div>
  );
}
