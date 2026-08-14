import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { Card, Icon, Logo } from "@/components/ui";
import { LoginBrandPanel } from "@/features/authentication/components/LoginBrandPanel";
import { LoginForm } from "@/features/authentication/components/LoginForm";
import { getCurrentUser } from "@/features/authentication/services/auth.service";

export const metadata: Metadata = {
  title: "Entrar — physical",
  description: "Acesso ao painel administrativo de RH da physical.",
};

export default async function LoginPage() {
  // Quem já tem sessão válida não vê o formulário de novo.
  const user = await getCurrentUser();
  if (user) redirect("/painel");

  return (
    <div className="grid min-h-dvh lg:grid-cols-2">
      <LoginBrandPanel />

      <main className="flex flex-col justify-center px-5 py-12 sm:px-10">
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
