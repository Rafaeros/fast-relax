import type { Metadata } from "next";
import Link from "next/link";
import { Alert, Card, Logo } from "@/components/ui";
import { describeRecoveryTokenAction } from "@/features/authentication/actions/recovery.actions";
import { ResetPasswordForm } from "@/features/authentication/components/ResetPasswordForm";

export const metadata: Metadata = {
  title: "Definir senha — physical",
};

/**
 * Convite de primeiro acesso e recuperação de senha, na mesma tela.
 *
 * <p>
 * Os dois casos chegam ao mesmo ponto — a pessoa provou controlar a caixa de
 * entrada e agora escolhe a senha. O que muda é o texto, e isso vem do próprio
 * token.
 *
 * <p>
 * Serve painel e colaborador: o link de e-mail não sabe de onde a pessoa veio, e
 * é o token que diz para qual login mandá-la no fim.
 */
export default async function RedefinirSenhaPage({
  searchParams,
}: {
  searchParams: Promise<{ token?: string }>;
}) {
  const { token } = await searchParams;
  // Valida antes de desenhar o formulário: pedir uma senha para depois recusar
  // no envio é o pior desfecho possível para quem já está sem acesso.
  const target = token ? await describeRecoveryTokenAction(token) : null;

  const invite = target?.purpose === "INVITE";
  const backTo = target?.audience === "COLLABORATOR" ? "/colaborador/entrar" : "/entrar";

  return (
    <main
      className="flex min-h-dvh flex-col justify-center px-5 py-12 sm:px-10"
      style={{ paddingTop: "calc(env(safe-area-inset-top) + 3rem)" }}
    >
      <div className="mx-auto flex w-full max-w-md flex-col gap-8">
        <Logo priority />

        {!target || !token ? (
          <>
            <div className="flex flex-col gap-2">
              <span className="eyebrow">Link inválido</span>
              <h1 className="font-display text-3xl text-ink-primary">Este link não vale mais</h1>
            </div>

            <Alert tone="warning" title="Peça um novo">
              Links de e-mail valem por tempo limitado e só podem ser usados uma vez. Se você já
              definiu a senha, é só entrar.
            </Alert>

            <Link href={backTo} className="text-center text-sm text-accent-soft underline">
              Voltar para a tela de entrada
            </Link>
          </>
        ) : (
          <>
            <div className="flex flex-col gap-2">
              <span className="eyebrow">{invite ? "Primeiro acesso" : "Recuperação de senha"}</span>
              <h1 className="font-display text-3xl text-ink-primary">
                {invite ? "Defina sua senha" : "Escolha uma nova senha"}
              </h1>
              <p className="text-sm leading-relaxed text-ink-secondary">
                Olá, {target.name.trim().split(/\s+/)[0]}.{" "}
                {invite
                  ? "Sua conta está pronta — falta só escolher a senha."
                  : "Depois de salvar, as sessões abertas nos outros aparelhos são encerradas."}
              </p>
            </div>

            <Card padding="lg">
              <ResetPasswordForm token={token} target={target} />
            </Card>
          </>
        )}
      </div>
    </main>
  );
}
