import type { Metadata } from "next";
import Link from "next/link";
import { Alert, Card, Icon, Logo } from "@/components/ui";
import { ForgotPasswordForm } from "@/features/authentication/components/ForgotPasswordForm";

export const metadata: Metadata = {
  title: "Recuperar senha — physical",
};

/**
 * Recuperação do colaborador.
 *
 * <p>
 * Pede o identificador da empresa além do e-mail: o e-mail dele só é único
 * dentro da empresa, e a mesma pessoa pode ser colaboradora de dois clientes.
 * É o mesmo identificador que ela digita para entrar, então não há
 * informação nova a pedir.
 */
export default function ColaboradorEsqueciSenhaPage() {
  return (
    <main
      className="flex min-h-dvh flex-col justify-center px-5 py-12 sm:px-10"
      style={{ paddingTop: "calc(env(safe-area-inset-top) + 3rem)" }}
    >
      <div className="mx-auto flex w-full max-w-md flex-col gap-8">
        <div className="flex items-center justify-between">
          <Logo priority />
          <Link
            href="/colaborador/entrar"
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
            Informe o identificador da empresa e o e-mail do seu cadastro para receber o link.
          </p>
        </div>

        <Card padding="lg">
          <ForgotPasswordForm audience="collaborator" />
        </Card>

        {/* Nem todo colaborador tem e-mail cadastrado — sem ele, o único caminho
            é o RH redefinir a senha. Dizer isso aqui evita a espera por um
            e-mail que nunca vai chegar. */}
        <Alert tone="info" title="Sem e-mail cadastrado?">
          Procure o RH da sua empresa: ele consegue gerar uma senha nova para você.
        </Alert>
      </div>
    </main>
  );
}
