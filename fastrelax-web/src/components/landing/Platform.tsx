import {
  Badge,
  Button,
  Card,
  Icon,
  Media,
  Section,
  SectionHeading,
} from "@/components/ui";
import type { IconName } from "@/components/ui";
import { assets } from "@/config/brand";

type PlatformBlock = {
  badge: string;
  title: string;
  description: string;
  bullets: { icon: IconName; text: string }[];
  image: string;
  imageAlt: string;
  cta: string;
  /** Inverte a ordem (imagem à esquerda) em telas grandes. */
  reversed?: boolean;
};

const BLOCKS: PlatformBlock[] = [
  {
    badge: "App do colaborador",
    title: "A pausa começa no celular",
    description:
      "O colaborador vê os horários livres, reserva a sessão e recebe o lembrete no celular. No horário marcado, inicia a massagem pelo próprio app.",
    bullets: [
      { icon: "calendar", text: "Agendamento em três toques nos horários liberados pela empresa" },
      { icon: "bell", text: "Notificação de lembrete quando a sessão está próxima" },
      { icon: "chair", text: "Na data e hora da reserva, o app inicia a sessão e ativa a cadeira" },
    ],
    image: assets.appMockup,
    imageAlt: "Aplicativo do colaborador physical em um celular",
    cta: "Ver o app do colaborador",
  },
  {
    badge: "Painel de RH",
    title: "Controle e indicadores de bem-estar",
    description:
      "O RH acompanha adesão por área, satisfação e status das cadeiras em um painel único — dados para justificar e ampliar o programa.",
    bullets: [
      { icon: "chart", text: "Uso por departamento, horário de pico e taxa de ocupação" },
      { icon: "users", text: "Gestão de colaboradores, escalas e agendamentos" },
      { icon: "wrench", text: "Status de manutenção e higienização de cada cadeira" },
    ],
    image: assets.dashboardMockup,
    imageAlt: "Painel de RH physical com dashboards de uso",
    cta: "Ver o painel de RH",
    reversed: true,
  },
];

export function Platform() {
  return (
    <Section id="plataforma" tone="raised" containerSize="wide">
      <SectionHeading
        eyebrow="Plataforma"
        title="Dois produtos, uma operação"
        description="Aplicativo para quem usa a cadeira, painel administrativo para quem cuida do programa."
      />

      <div className="mt-16 flex flex-col gap-16">
        {BLOCKS.map((block) => (
          <div
            key={block.title}
            className="grid items-center gap-10 lg:grid-cols-2 lg:gap-16"
          >
            <div className={block.reversed ? "lg:order-2" : undefined}>
              <div className="flex flex-col gap-5">
                <Badge tone="accent">{block.badge}</Badge>
                <h3 className="font-display text-2xl leading-tight text-ink-primary sm:text-3xl">
                  {block.title}
                </h3>
                <p className="max-w-lg text-sm leading-relaxed text-ink-secondary sm:text-base">
                  {block.description}
                </p>

                <ul className="flex flex-col gap-3.5">
                  {block.bullets.map((bullet) => (
                    <li key={bullet.text} className="flex items-start gap-3">
                      <span className="mt-0.5 grid h-8 w-8 shrink-0 place-items-center rounded-control border border-line bg-surface-card text-accent-soft">
                        <Icon name={bullet.icon} className="h-4 w-4" />
                      </span>
                      <span className="text-sm leading-relaxed text-ink-secondary">
                        {bullet.text}
                      </span>
                    </li>
                  ))}
                </ul>

                <div>
                  <Button
                    variant="secondary"
                    trailingIcon={<Icon name="arrowRight" className="h-4 w-4" />}
                  >
                    {block.cta}
                  </Button>
                </div>
              </div>
            </div>

            <Card
              variant="glass"
              padding="none"
              className={block.reversed ? "lg:order-1" : undefined}
            >
              <Media
                src={block.image}
                alt={block.imageAlt}
                aspect="aspect-[4/3]"
                sizes="(max-width: 1024px) 100vw, 50vw"
              />
            </Card>
          </div>
        ))}
      </div>
    </Section>
  );
}
