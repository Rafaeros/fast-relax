import type { ReactNode } from "react";
import { redirect } from "next/navigation";
import { Badge, Logo } from "@/components/ui";
import { PanelBreadcrumb } from "@/components/layout/PanelBreadcrumb";
import { PanelSidebar } from "@/components/layout/PanelSidebar";
import { LogoutButton } from "@/features/authentication/components/LogoutButton";
import { getCurrentUser } from "@/features/authentication/services/auth.service";

/**
 * Shell das rotas do painel. O guard fica aqui: qualquer página sob `/painel`
 * só renderiza com sessão válida — sem token, redireciona ao login.
 *
 * A altura é travada em uma tela (`h-dvh` + `overflow-hidden`): a sidebar vai
 * de topo a base, e só as áreas internas rolam. A topbar pertence à coluna de
 * conteúdo, então não passa por cima da sidebar.
 */
export default async function PainelLayout({ children }: { children: ReactNode }) {
  const user = await getCurrentUser();
  if (!user) redirect("/entrar");

  return (
    <div className="flex h-dvh flex-col overflow-hidden lg:flex-row">
      <PanelSidebar />

      <div className="flex min-h-0 min-w-0 flex-1 flex-col">
        <header className="shrink-0 border-b border-line bg-surface-nav">
          <div className="flex h-16 items-center justify-between gap-4 px-5 sm:px-8">
            <div className="flex min-w-0 items-center gap-3">
              {/* A logo mora na sidebar a partir de lg; aqui ela cobre o mobile. */}
              <Logo height={22} variant="mark" className="lg:hidden" />
              <PanelBreadcrumb />
            </div>

            <div className="flex shrink-0 items-center gap-3">
              <Badge tone="neutral" className="hidden sm:inline-flex">
                {user.role === "ADMIN" ? "Admin" : "RH"}
              </Badge>
              <span className="hidden text-sm text-ink-secondary lg:inline">{user.name}</span>
              <LogoutButton />
            </div>
          </div>
        </header>

        {/* Rolagem da página acontece aqui dentro, nunca no body. */}
        <main className="min-h-0 flex-1 overflow-y-auto px-5 py-8 sm:px-8">{children}</main>
      </div>
    </div>
  );
}
