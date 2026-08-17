import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { Card, Icon, Logo } from "@/components/ui";
import { LoginBrandPanel } from "@/features/authentication/components/LoginBrandPanel";
import { CollaboratorLoginForm } from "@/features/collaborator-portal/components/CollaboratorLoginForm";
import { getCurrentCollaborator } from "@/features/collaborator-portal/services/portal.service";

export const metadata: Metadata = {
  title: "Entrar — physical",
  description: "Acesso do colaborador para agendar massagens.",
};

/** Mesma composição do login do RH, com a mensagem voltada a quem usa a cadeira. */
const HIGHLIGHTS = [
  { icon: "calendar" as const, text: "Agende sua massagem em segundos" },
  { icon: "clock" as const, text: "Horários dentro da sua janela permitida" },
  { icon: "heart" as const, text: "Acompanhe seu histórico de sessões" },
];

export default async function CollaboratorLoginPage() {
  // Quem já tem sessão válida não vê o formulário de novo.
  const collaborator = await getCurrentCollaborator();
  if (collaborator) redirect("/colaborador");

  return (
    // Duas colunas a partir de lg, igual ao login do painel: no celular sobra
    // só o formulário, que é como a maioria dos colaboradores entra.
    <div className="grid min-h-dvh lg:grid-cols-2">
      <LoginBrandPanel
        eyebrow="Área do colaborador"
        title={
          <>
            Sua pausa,
            <br />
            <span className="text-gradient-accent">no seu tempo.</span>
          </>
        }
        description="Agende sua massagem, acompanhe suas sessões e aproveite o intervalo do jeito que ele deveria ser."
        highlights={HIGHLIGHTS}
      />

      <main
        className="flex flex-col justify-center px-5 py-12 sm:px-10"
        style={{ paddingTop: "calc(env(safe-area-inset-top) + 3rem)" }}
      >
        <div className="mx-auto flex w-full max-w-md flex-col gap-8">
          <div className="flex items-center justify-between">
            <Logo priority />
            <Link
              href="/"
              className="inline-flex items-center gap-1.5 text-xs font-semibold uppercase tracking-[0.14em] text-ink-secondary transition-colors hover:text-ink-primary"
            >
              <Icon name="arrowLeft" className="h-4 w-4" />
              Voltar ao site
            </Link>
          </div>

          <div className="flex flex-col gap-2">
            <span className="eyebrow">Área do colaborador</span>
            <h1 className="font-display text-3xl text-ink-primary">Bem-vindo</h1>
            <p className="text-sm leading-relaxed text-ink-secondary">
              Informe seu CPF para agendar sua massagem e acompanhar suas sessões.
            </p>
          </div>

          <Card padding="lg">
            <CollaboratorLoginForm />
          </Card>

          <p className="text-center text-xs text-ink-tertiary">
            É do RH?{" "}
            <Link href="/entrar" className="text-accent-soft underline">
              Acessar o painel
            </Link>
          </p>
        </div>
      </main>
    </div>
  );
}
