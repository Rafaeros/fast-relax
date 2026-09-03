import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { Alert, Card, Icon, Logo } from "@/components/ui";
import { LoginBrandPanel } from "@/features/authentication/components/LoginBrandPanel";
import {
  LOGIN_CARD,
  LOGIN_DESCRIPTION,
  LOGIN_EYEBROW,
  LOGIN_HEADING,
  LOGIN_MAIN_SAFE_AREA,
  LOGIN_SHELL,
  LOGIN_STACK,
  LOGIN_TITLE,
} from "@/features/authentication/lib/login-layout";
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

export default async function CollaboratorLoginPage({
  searchParams,
}: {
  searchParams: Promise<{ senha?: string }>;
}) {
  // Vem do redirecionamento pós-redefinição: confirma que deu certo, já que a
  // ação não abre sessão sozinha.
  const { senha } = await searchParams;
  // Quem já tem sessão válida não vê o formulário de novo.
  const collaborator = await getCurrentCollaborator();
  if (collaborator) redirect("/colaborador");

  return (
    // Duas colunas a partir de lg, igual ao login do painel: no celular sobra
    // só o formulário, que é como a maioria dos colaboradores entra.
    <div className={LOGIN_SHELL}>
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

      {/*
        A safe area do topo vive numa classe (e não em `style`), senão o inline
        venceria o `lg:pt-*` da densidade de desktop por especificidade.
      */}
      <main className={LOGIN_MAIN_SAFE_AREA}>
        <div className={LOGIN_STACK}>
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

          <div className={LOGIN_HEADING}>
            <span className={LOGIN_EYEBROW}>Área do colaborador</span>
            <h1 className={LOGIN_TITLE}>Bem-vindo</h1>
            <p className={LOGIN_DESCRIPTION}>
              Informe o identificador da empresa, seu CPF e sua senha para
              agendar e acompanhar suas sessões.
            </p>
          </div>

          {senha === "ok" && (
            <Alert tone="success" title="Senha definida">
              Entre com a senha que você acabou de escolher.
            </Alert>
          )}

          <Card padding="lg" className={LOGIN_CARD}>
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
