import type { ReactNode } from "react";
import { Icon, Media } from "@/components/ui";
import type { IconName } from "@/components/ui";
import { assets, brand } from "@/config/brand";

export type LoginHighlight = { icon: IconName; text: string };

export type LoginBrandPanelProps = {
  eyebrow?: string;
  title?: ReactNode;
  description?: string;
  highlights?: LoginHighlight[];
};

const PANEL_HIGHLIGHTS: LoginHighlight[] = [
  { icon: "chart", text: "Indicadores de uso e satisfação por departamento" },
  { icon: "users", text: "Gestão de colaboradores, escalas e agendamentos" },
  { icon: "wrench", text: "Status de manutenção de cada cadeira" },
];

const DEFAULT_TITLE = (
  <>
    O bem-estar do time,
    <br />
    <span className="text-gradient-accent">em números.</span>
  </>
);

/**
 * Coluna institucional da tela de login — decorativa, sem lógica de auth.
 *
 * <p>
 * O conteúdo é parametrizável porque as duas portas de entrada (painel do RH e
 * app do colaborador) compartilham a mesma composição visual, mas falam com
 * públicos diferentes. Os valores padrão são os do painel.
 *
 * <p>
 * Só existe a partir de `lg`, e nesse ponto a tela toda é travada em 100dvh —
 * por isso padding, respiro e título acompanham a altura da viewport em vez de
 * serem fixos: num laptop baixo a coluna encolhe junto em vez de empurrar o topo
 * (ela é ancorada no rodapé) para fora do `overflow-hidden`.
 */
export function LoginBrandPanel({
  eyebrow = "Painel administrativo",
  title = DEFAULT_TITLE,
  description = `${brand.tagline}. Acompanhe adesão, agenda e manutenção das cadeiras em um lugar só.`,
  highlights = PANEL_HIGHLIGHTS,
}: LoginBrandPanelProps) {
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

      <div className="flex flex-col gap-[clamp(0.75rem,2.2dvh,1.5rem)] p-[clamp(1.5rem,4.5dvh,3rem)]">
        <span className="eyebrow">{eyebrow}</span>
        <h2 className="font-display text-[length:clamp(1.75rem,4.4dvh,2.25rem)] leading-tight text-ink-primary">
          {title}
        </h2>
        <p className="max-w-sm text-sm leading-snug text-ink-secondary">{description}</p>

        <ul className="flex flex-col gap-[clamp(0.5rem,1.4dvh,0.75rem)]">
          {highlights.map((highlight) => (
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
