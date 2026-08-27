import type { Route } from "next";
import { ButtonLink, Card, Icon, Logo } from "@/components/ui";

/**
 * Porta de entrada do celular.
 *
 * <p>
 * Substitui a navbar abaixo de {@code lg}. No celular aquela barra fixa custava
 * altura permanente para entregar um menu escondido atrás do hambúrguer — e
 * quem abre o site no telefone quase sempre quer uma coisa só: entrar. Este
 * cartão coloca as duas áreas no centro da primeira tela, sem toque nenhum
 * antes.
 *
 * <p>
 * O conteúdo institucional continua abaixo, para quem chegou sem conta e quer
 * conhecer o produto rolando a página.
 */
export function MobileAccessCard() {
  return (
    <section
      // min-h-dvh com a área segura descontada: o cartão ocupa a primeira tela
      // inteira sem ficar sob a barra de status nem sob a barra de gestos.
      className="flex min-h-dvh flex-col items-center justify-center gap-6 px-4 py-10 lg:hidden"
      style={{
        paddingTop: "calc(2.5rem + env(safe-area-inset-top))",
        paddingBottom: "calc(2.5rem + env(safe-area-inset-bottom))",
      }}
    >
      <Logo priority height={32} />

      <Card
        padding="lg"
        className="flex w-full max-w-sm flex-col gap-5 border-accent/30 bg-gradient-to-br from-accent-strong/20 via-surface-card to-surface-card"
      >
        <div className="flex flex-col gap-1 text-center">
          <h1 className="font-display text-2xl text-ink-primary">Bem-vindo</h1>
          <p className="text-sm text-ink-secondary">Escolha a sua área para entrar.</p>
        </div>

        <div className="flex flex-col gap-3">
          {/* Colaborador primeiro e em destaque: é quem abre o app todo dia,
              enquanto o RH entra de vez em quando. */}
          <ButtonLink
            href={"/colaborador/entrar" as Route}
            variant="primary"
            size="md"
            fullWidth
            leadingIcon={<Icon name="users" className="h-4 w-4" />}
          >
            Área do colaborador
          </ButtonLink>

          <ButtonLink
            href={"/entrar" as Route}
            variant="secondary"
            size="md"
            fullWidth
            leadingIcon={<Icon name="lock" className="h-4 w-4" />}
          >
            Área do cliente
          </ButtonLink>
        </div>
      </Card>

      <a
        href="#cadeira"
        className="flex flex-col items-center gap-1 text-xs text-ink-tertiary"
      >
        Conhecer o programa
        {/* Não há chevron para baixo no conjunto de ícones; a seta girada
            aponta para o conteúdo que segue abaixo. */}
        <Icon name="arrowRight" className="h-4 w-4 rotate-90" />
      </a>
    </section>
  );
}
