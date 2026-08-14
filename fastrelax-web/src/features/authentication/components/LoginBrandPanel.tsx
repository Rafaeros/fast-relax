import { Icon, Media } from "@/components/ui";
import type { IconName } from "@/components/ui";
import { assets, brand } from "@/config/brand";

const HIGHLIGHTS: { icon: IconName; text: string }[] = [
  { icon: "chart", text: "Indicadores de uso e satisfação por departamento" },
  { icon: "users", text: "Gestão de colaboradores, escalas e agendamentos" },
  { icon: "wrench", text: "Status de manutenção de cada cadeira" },
];

/** Coluna institucional da tela de login — decorativa, sem lógica de auth. */
export function LoginBrandPanel() {
  return (
    <aside className="relative hidden overflow-hidden lg:flex lg:flex-col lg:justify-end">
      <div className="absolute inset-0 -z-10">
        <Media
          src={assets.heroBackground}
          alt=""
          aspect="h-full"
          className="h-full"
          sizes="50vw"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-bg-950 via-bg-950/85 to-bg-950/55" />
        <div className="ambient-glow absolute inset-x-0 bottom-0 h-2/3" />
      </div>

      <div className="flex flex-col gap-6 p-12">
        <span className="eyebrow">Painel administrativo</span>
        <h2 className="font-display text-4xl leading-tight text-ink-primary">
          O bem-estar do time,
          <br />
          <span className="text-gradient-accent">em números.</span>
        </h2>
        <p className="max-w-sm text-sm leading-relaxed text-ink-secondary">
          {brand.tagline}. Acompanhe adesão, agenda e manutenção das cadeiras em um lugar só.
        </p>

        <ul className="flex flex-col gap-3 pt-2">
          {HIGHLIGHTS.map((highlight) => (
            <li key={highlight.text} className="flex items-center gap-3">
              <span className="grid h-8 w-8 shrink-0 place-items-center rounded-control border border-line bg-surface-card/70 text-accent-soft">
                <Icon name={highlight.icon} className="h-4 w-4" />
              </span>
              <span className="text-sm text-ink-secondary">{highlight.text}</span>
            </li>
          ))}
        </ul>
      </div>
    </aside>
  );
}
