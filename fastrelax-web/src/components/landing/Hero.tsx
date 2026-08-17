import type { Route } from "next";
import { ButtonLink, Container, Icon, Media, StatGroup } from "@/components/ui";
import { assets } from "@/config/brand";

const STATS = [
  { value: "12 min", label: "Sessão média", icon: <Icon name="clock" /> },
  { value: "+2.400", label: "Colaboradores", icon: <Icon name="users" /> },
  { value: "4,9/5", label: "Satisfação", icon: <Icon name="heart" /> },
];

export function Hero() {
  return (
    // pt menor no celular: a navbar tem 64px, e 128px de folga empurrava o
    // título para fora da primeira dobra em telas de 640px de altura.
    <section id="inicio" className="relative overflow-hidden pt-24 pb-14 sm:pt-40 sm:pb-28">
      {/* Fundo: foto do ambiente + brilho ambiente + fade para o corpo da página */}
      <div className="absolute inset-0 -z-10">
        <Media
          src={assets.heroBackground}
          alt=""
          aspect="h-full"
          className="h-full"
          sizes="100vw"
          priority
        />
        <div className="absolute inset-0 bg-gradient-to-r from-bg-950 via-bg-950/85 to-bg-950/40" />
        <div className="absolute inset-0 bg-gradient-to-t from-bg-950 via-transparent to-bg-950/70" />
        <div className="ambient-glow absolute inset-x-0 bottom-0 h-2/3" />
      </div>

      <Container size="wide">
        <div className="grid items-center gap-10 sm:gap-12 lg:grid-cols-[minmax(0,1fr)_minmax(0,1.05fr)]">
          <div className="flex flex-col gap-5 sm:gap-7">
            <span className="eyebrow">Bem-estar corporativo</span>

            <h1 className="font-display text-[2.5rem] leading-[1.05] text-ink-primary sm:text-6xl">
              Descanso
              <br />
              <span className="text-gradient-accent">Redefinido.</span>
            </h1>

            <p className="max-w-md text-base leading-relaxed text-ink-secondary">
              Cadeiras de massagem profissionais dentro da sua empresa, com agendamento pelo app do
              colaborador e acompanhamento completo pelo RH.
            </p>

            {/*
              Empilhados e de largura total no celular: lado a lado, cada botão
              ficava com metade de 335px e o rótulo quebrava em duas linhas.
            */}
            <div className="flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-center">
              <ButtonLink
                href={"/colaborador/entrar" as Route}
                size="lg"
                fullWidth
                className="sm:w-auto"
                trailingIcon={<Icon name="arrowRight" className="h-4 w-4" />}
              >
                Agendar massagem
              </ButtonLink>
              <ButtonLink
                href={"#plataforma" as Route}
                variant="secondary"
                size="lg"
                fullWidth
                className="sm:w-auto"
                leadingIcon={<Icon name="play" className="h-4 w-4" />}
              >
                Conhecer a plataforma
              </ButtonLink>
            </div>

            <div className="hairline mt-2 max-w-md" />

            <StatGroup items={STATS} />
          </div>

          <div className="relative">
            <div className="ambient-glow absolute -inset-10 -z-10" />
            <Media
              src={assets.heroChair}
              alt="Cadeira de massagem physical"
              aspect="aspect-[4/3]"
              fit="contain"
              className="bg-transparent"
              sizes="(max-width: 1024px) 100vw, 55vw"
              priority
            />
          </div>
        </div>
      </Container>
    </section>
  );
}
