import type { Route } from "next";
import { ButtonLink, Card, Icon, Section, SectionHeading } from "@/components/ui";

type Area = {
  eyebrow: string;
  title: string;
  description: string;
  href: Route;
  cta: string;
  icon: "users" | "shield";
  primary: boolean;
};

const AREAS: Area[] = [
  {
    eyebrow: "Colaborador",
    title: "Agende sua massagem",
    description:
      "Entre com o identificador da sua empresa, seu CPF e sua senha, escolha um horário livre dentro da sua janela e acompanhe suas sessões.",
    href: "/colaborador/entrar" as Route,
    cta: "Área do colaborador",
    icon: "users",
    // Destaque para quem usa todo dia; o RH entra de vez em quando.
    primary: true,
  },
  {
    eyebrow: "RH e administração",
    title: "Gerencie o programa",
    description:
      "Cadastre colaboradores, configure horários e acompanhe os indicadores de uso das cadeiras.",
    href: "/entrar" as Route,
    cta: "Área do cliente",
    icon: "shield",
    primary: false,
  },
];

/**
 * Portas de entrada do sistema, na própria página.
 *
 * <p>
 * A navbar também leva às duas áreas, mas some atrás do menu em telas
 * pequenas. Aqui elas ficam visíveis em qualquer largura, logo depois da seção
 * que apresenta o app e o painel — quem acabou de entender o produto encontra
 * o acesso sem voltar ao topo.
 */
export function AccessAreas() {
  return (
    <Section id="acessos">
      <SectionHeading
        eyebrow="Acesso"
        title="Entre na sua área"
        description="Cada perfil tem a própria porta de entrada."
      />

      <div className="mt-10 grid gap-4 sm:grid-cols-2">
        {AREAS.map((area) => (
          <Card
            key={area.href}
            padding="lg"
            className={
              area.primary
                ? "flex flex-col gap-4 border-accent/30 bg-gradient-to-br from-accent-strong/20 via-surface-card to-surface-card"
                : "flex flex-col gap-4"
            }
          >
            <span className="flex h-11 w-11 items-center justify-center rounded-full bg-surface-hover">
              <Icon
                name={area.icon}
                className={area.primary ? "h-5 w-5 text-accent-soft" : "h-5 w-5 text-ink-muted"}
              />
            </span>

            <div className="flex flex-col gap-1">
              <span className="eyebrow">{area.eyebrow}</span>
              <h3 className="font-display text-xl text-ink-primary">{area.title}</h3>
              <p className="text-sm leading-relaxed text-ink-secondary">{area.description}</p>
            </div>

            {/* mt-auto alinha os botões dos dois cartões quando as descrições
                têm alturas diferentes. */}
            <ButtonLink
              href={area.href}
              variant={area.primary ? "primary" : "secondary"}
              size="md"
              fullWidth
              className="mt-auto"
            >
              {area.cta}
            </ButtonLink>
          </Card>
        ))}
      </div>
    </Section>
  );
}
