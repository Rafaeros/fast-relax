import { Container, Icon, Media } from "@/components/ui";
import type { IconName } from "@/components/ui";
import { assets } from "@/config/brand";

const REASONS: { icon: IconName; title: string; description: string }[] = [
  {
    icon: "sparkle",
    title: "Equipamento premium",
    description: "Cadeiras profissionais com acabamento e conforto de nível hoteleiro.",
  },
  {
    icon: "shield",
    title: "Dados protegidos",
    description: "Acesso individual pelo app e histórico de sessões visível só para quem precisa.",
  },
  {
    icon: "users",
    title: "Adesão real",
    description: "Fluxo simples faz o colaborador voltar — não é benefício de gaveta.",
  },
  {
    icon: "wrench",
    title: "Operação sem esforço",
    description: "Instalação, manutenção e suporte por nossa conta, do primeiro dia em diante.",
  },
];

export function WhyChoose() {
  return (
    <section className="relative overflow-hidden py-24 sm:py-32">
      <div className="absolute inset-0 -z-10">
        <Media src={assets.ctaBackground} alt="" aspect="h-full" className="h-full" sizes="100vw" />
        <div className="absolute inset-0 bg-bg-950/85" />
      </div>

      <Container size="wide">
        <div className="flex flex-col items-center gap-4 text-center">
          <h2 className="font-display text-3xl text-ink-primary sm:text-4xl">
            Por que escolher a physical?
          </h2>
          <span className="h-px w-16 bg-accent-strong" />
        </div>

        <div className="mt-14 grid gap-10 sm:grid-cols-2 lg:grid-cols-4">
          {REASONS.map((reason) => (
            <div key={reason.title} className="flex flex-col items-center gap-3 text-center">
              <Icon name={reason.icon} className="h-8 w-8 text-accent-soft" />
              <h3 className="text-sm font-semibold text-ink-primary">{reason.title}</h3>
              <p className="max-w-xs text-xs leading-relaxed text-ink-secondary">
                {reason.description}
              </p>
            </div>
          ))}
        </div>
      </Container>
    </section>
  );
}
