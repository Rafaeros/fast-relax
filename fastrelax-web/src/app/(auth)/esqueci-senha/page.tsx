import type { Metadata } from "next";
import Link from "next/link";
import { Card, Icon, Logo } from "@/components/ui";
import { ForgotPasswordForm } from "@/features/authentication/components/ForgotPasswordForm";

export const metadata: Metadata = {
  title: "Recuperar senha — physical",
};

/** Recuperação do painel: o e-mail é único no sistema inteiro. */
export default function EsqueciSenhaPage() {
  return (
    <main
      className="flex min-h-dvh flex-col justify-center px-5 py-12 sm:px-10"
      style={{ paddingTop: "calc(env(safe-area-inset-top) + 3rem)" }}
    >
      <div className="mx-auto flex w-full max-w-md flex-col gap-8">
        <div className="flex items-center justify-between">
          <Logo priority />
          <Link
            href="/entrar"
            className="inline-flex items-center gap-1.5 text-xs font-semibold uppercase tracking-[0.14em] text-ink-secondary transition-colors hover:text-ink-primary"
          >
            <Icon name="arrowLeft" className="h-4 w-4" />
            Voltar
          </Link>
        </div>

        <div className="flex flex-col gap-2">
          <span className="eyebrow">Recuperação de senha</span>
          <h1 className="font-display text-3xl text-ink-primary">Esqueceu a senha?</h1>
          <p className="text-sm leading-relaxed text-ink-secondary">
            Informe o e-mail cadastrado e enviaremos um link para você escolher uma nova.
          </p>
        </div>

        <Card padding="lg">
          <ForgotPasswordForm audience="user" />
        </Card>
      </div>
    </main>
  );
}
