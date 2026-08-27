import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { Alert, Card, Logo } from "@/components/ui";
import { PasswordSetupForm } from "@/features/authentication/components/PasswordSetupForm";
import { collaboratorFirstAccessPasswordAction } from "@/features/collaborator-portal/actions/portal.actions";
import { getCurrentCollaborator } from "@/features/collaborator-portal/services/portal.service";

export const metadata: Metadata = {
  title: "Definir senha — physical",
};

/**
 * Primeiro acesso do colaborador.
 *
 * <p>
 * Mora fora do grupo `(app)` de propósito: o layout de lá redireciona para cá
 * quem ainda tem senha temporária, e ter esta tela dentro dele criaria um laço.
 * É também a única rota do app que o backend libera nesse estado — todo o resto
 * responde 403 até a senha ser trocada.
 */
export default async function DefinirSenhaPage() {
  const collaborator = await getCurrentCollaborator();
  if (!collaborator) redirect("/colaborador/entrar");

  // Senha já definida: não há o que fazer aqui, e deixar a tela acessível daria
  // a impressão de que a troca ficou pendente.
  if (!collaborator.mustChangePassword) redirect("/colaborador");

  return (
    <main
      className="flex min-h-dvh flex-col justify-center px-5 py-12 sm:px-10"
      style={{ paddingTop: "calc(env(safe-area-inset-top) + 3rem)" }}
    >
      <div className="mx-auto flex w-full max-w-md flex-col gap-8">
        <Logo priority />

        <div className="flex flex-col gap-2">
          <span className="eyebrow">Primeiro acesso</span>
          <h1 className="font-display text-3xl text-ink-primary">Defina sua senha</h1>
          <p className="text-sm leading-relaxed text-ink-secondary">
            Olá, {collaborator.name.trim().split(/\s+/)[0]}. A senha que o RH entregou é
            temporária — escolha a sua para liberar o app.
          </p>
        </div>

        <Alert tone="info" title="Por que agora">
          Enquanto a senha temporária valer, o acesso fica limitado a esta tela.
        </Alert>

        <Card padding="lg">
          <PasswordSetupForm
            mode="first-access"
            action={collaboratorFirstAccessPasswordAction}
          />
        </Card>
      </div>
    </main>
  );
}
