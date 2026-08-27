import type { ReactNode } from "react";
import { redirect } from "next/navigation";
import { Badge, Logo } from "@/components/ui";
import { LogoutButton } from "@/features/authentication/components/LogoutButton";
import { CollaboratorSidebar } from "@/features/collaborator-portal/components/CollaboratorSidebar";
import { CollaboratorTabBar } from "@/features/collaborator-portal/components/CollaboratorTabBar";
import { getCurrentCollaborator } from "@/features/collaborator-portal/services/portal.service";

/**
 * Shell do app do colaborador.
 *
 * <p>
 * O guard fica aqui: sem sessão de colaborador, volta ao login. Um token de
 * usuário do painel também cai fora — `/collaborators/me` só responde para quem
 * entrou por CPF. A tela de login mora fora deste grupo de rotas, senão o
 * próprio guard a redirecionaria em laço.
 *
 * <p>
 * Mesma estrutura do painel do RH no desktop — sidebar de altura total e header
 * na coluna de conteúdo. No celular a sidebar some e a navegação vira barra de
 * abas fixa no rodapé, que é o formato que o app empacotado precisa ter.
 */
export default async function CollaboratorAppLayout({ children }: { children: ReactNode }) {
  const collaborator = await getCurrentCollaborator();
  if (!collaborator) redirect("/colaborador/entrar");

  // Com senha temporária o backend bloqueia todo o resto da API: qualquer tela
  // daqui renderizaria erro, então o caminho é um só.
  if (collaborator.mustChangePassword) redirect("/colaborador/definir-senha");

  const firstName = collaborator.name.trim().split(/\s+/)[0];

  return (
    <div className="flex h-dvh flex-col overflow-hidden lg:flex-row">
      <CollaboratorSidebar />

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
              <div className="min-w-0">
                <p className="truncate text-sm font-semibold text-ink-primary">Olá, {firstName}!</p>
                <p className="truncate text-xs text-ink-tertiary">
                  {collaborator.departmentName ?? "Colaborador"}
                </p>
              </div>
            </div>

            <div className="flex shrink-0 items-center gap-2 sm:gap-3">
              <Badge tone="neutral" className="hidden sm:inline-flex">
                Colaborador
              </Badge>
              <LogoutButton origin="collaborator" />
            </div>
          </div>
        </header>

        {/*
          Rolagem da página acontece aqui dentro, nunca no body.
          O padding inferior no mobile reserva a altura da barra de abas mais a
          área segura, senão o último item da lista fica coberto por ela.
        */}
        <main className="min-h-0 flex-1 overflow-y-auto px-4 py-5 pb-[calc(4.5rem+env(safe-area-inset-bottom))] sm:px-8 lg:py-8 lg:pb-8">
          <div className="mx-auto w-full max-w-3xl lg:max-w-4xl">{children}</div>
        </main>
      </div>

      <CollaboratorTabBar />
    </div>
  );
}
