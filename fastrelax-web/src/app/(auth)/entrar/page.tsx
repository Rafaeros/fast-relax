import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { Alert, Card, Icon, Logo } from "@/components/ui";
import { LoginBrandPanel } from "@/features/authentication/components/LoginBrandPanel";
import { LoginForm } from "@/features/authentication/components/LoginForm";
import {
  LOGIN_CARD,
  LOGIN_DESCRIPTION,
  LOGIN_EYEBROW,
  LOGIN_HEADING,
  LOGIN_MAIN,
  LOGIN_SHELL,
  LOGIN_STACK,
  LOGIN_TITLE,
} from "@/features/authentication/lib/login-layout";
import { getCurrentUser } from "@/features/authentication/services/auth.service";

export const metadata: Metadata = {
  title: "Entrar — physical",
  description: "Acesso ao painel da physical.",
};

export default async function LoginPage({
  searchParams,
}: {
  searchParams: Promise<{ senha?: string }>;
}) {
  // Vem do redirecionamento pós-redefinição: confirma que deu certo, já que a
  // ação não abre sessão sozinha.
  const { senha } = await searchParams;
  // Quem já tem sessão válida não vê o formulário de novo.
  const user = await getCurrentUser();
  if (user) redirect("/painel");

  return (
    <div className={LOGIN_SHELL}>
      <LoginBrandPanel />

      <main className={LOGIN_MAIN}>
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
            <span className={LOGIN_EYEBROW}>Área do cliente</span>
            <h1 className={LOGIN_TITLE}>Acesse seu painel</h1>
            <p className={LOGIN_DESCRIPTION}>
              Entre com as credenciais fornecidas pela sua empresa para gerenciar sessões,
              colaboradores e indicadores.
            </p>
          </div>

          {senha === "ok" && (
            <Alert tone="success" title="Senha definida">
              Entre com a senha que você acabou de escolher.
            </Alert>
          )}

          <Card padding="lg" className={LOGIN_CARD}>
            <LoginForm />
          </Card>

          <p className="text-center text-xs text-ink-tertiary">
            Problemas para entrar? Fale com o administrador da sua empresa.
          </p>
        </div>
      </main>
    </div>
  );
}
