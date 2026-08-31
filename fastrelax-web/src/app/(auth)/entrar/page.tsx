import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { Alert, Card, Icon, Logo } from "@/components/ui";
import { LoginBrandPanel } from "@/features/authentication/components/LoginBrandPanel";
import { LoginForm } from "@/features/authentication/components/LoginForm";
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
    <div className="grid min-h-dvh lg:h-dvh lg:grid-cols-2 lg:overflow-hidden">
      <LoginBrandPanel />

      <main className="flex flex-col justify-center px-5 py-12 sm:px-10 lg:h-full lg:overflow-y-auto">
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
            <span className="eyebrow">Área do cliente</span>
            <h1 className="font-display text-3xl text-ink-primary">Acesse seu painel</h1>
            <p className="text-sm leading-relaxed text-ink-secondary">
              Entre com as credenciais fornecidas pela sua empresa para gerenciar sessões,
              colaboradores e indicadores.
            </p>
          </div>

          {senha === "ok" && (
            <Alert tone="success" title="Senha definida">
              Entre com a senha que você acabou de escolher.
            </Alert>
          )}

          <Card padding="lg">
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
