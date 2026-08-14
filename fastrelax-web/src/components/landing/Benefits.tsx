import { FeatureCard, Icon, Section, SectionHeading } from "@/components/ui";

const BENEFITS = [
  {
    icon: <Icon name="calendar" />,
    title: "Agenda no app",
    description:
      "O colaborador reserva o próprio horário pelo aplicativo, dentro das faixas liberadas pela empresa.",
  },
  {
    icon: <Icon name="bell" />,
    title: "Lembretes automáticos",
    description:
      "Notificações avisam quando a sessão está próxima, para ninguém perder o horário reservado.",
  },
  {
    icon: <Icon name="chair" />,
    title: "Início pelo aplicativo",
    description:
      "Na data e hora da reserva, o colaborador toca em iniciar no app e a cadeira é ativada na hora.",
  },
  {
    icon: <Icon name="shield" />,
    title: "Suporte e manutenção",
    description:
      "Higienização, monitoramento de uso e manutenção preventiva acompanhados dentro do painel de RH.",
  },
];

export function Benefits() {
  return (
    <Section id="cadeira" tone="raised" containerSize="wide">
      <SectionHeading
        eyebrow="Experiência completa"
        title="Cuidado que cabe na jornada de trabalho"
        description="Cada detalhe foi pensado para que a pausa aconteça de fato — sem burocracia para o colaborador e com controle para o RH."
      />

      <div className="mt-14 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
        {BENEFITS.map((benefit) => (
          <FeatureCard key={benefit.title} {...benefit} />
        ))}
      </div>
    </Section>
  );
}
