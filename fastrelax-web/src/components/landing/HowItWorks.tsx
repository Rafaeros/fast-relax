import { Card, Icon, Section, SectionHeading } from "@/components/ui";
import type { IconName } from "@/components/ui";

const STEPS: { step: string; icon: IconName; title: string; description: string }[] = [
  {
    step: "01",
    icon: "calendar",
    title: "Agenda",
    description: "O colaborador escolhe o horário livre e o programa direto pelo app.",
  },
  {
    step: "02",
    icon: "bell",
    title: "É lembrado",
    description: "O app envia notificação de lembrete conforme a sessão se aproxima.",
  },
  {
    step: "03",
    icon: "chair",
    title: "Inicia e relaxa",
    description: "Na data e hora da reserva, ele inicia pelo app e a cadeira é ativada.",
  },
  {
    step: "04",
    icon: "chart",
    title: "Acompanha",
    description: "Uso e satisfação chegam ao painel de RH em tempo real.",
  },
];

export function HowItWorks() {
  return (
    <Section id="como-funciona" containerSize="wide">
      <SectionHeading
        eyebrow="Como funciona"
        title="Do agendamento ao indicador, em quatro passos"
      />

      <ol className="mt-14 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
        {STEPS.map((step) => (
          <li key={step.step}>
            <Card padding="lg" className="h-full">
              <div className="flex flex-col gap-4">
                <div className="flex items-center justify-between">
                  <span className="grid h-10 w-10 place-items-center rounded-control border border-line bg-bg-900 text-accent-soft">
                    <Icon name={step.icon} className="h-5 w-5" />
                  </span>
                  <span className="font-display text-3xl text-line">{step.step}</span>
                </div>
                <h3 className="text-base font-semibold text-ink-primary">{step.title}</h3>
                <p className="text-sm leading-relaxed text-ink-secondary">{step.description}</p>
              </div>
            </Card>
          </li>
        ))}
      </ol>
    </Section>
  );
}
