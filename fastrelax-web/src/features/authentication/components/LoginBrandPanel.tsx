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

      <div className="flex flex-col gap-6 p-12">
        <span className="eyebrow">{eyebrow}</span>
        <h2 className="font-display text-4xl leading-tight text-ink-primary">{title}</h2>
        <p className="max-w-sm text-sm leading-relaxed text-ink-secondary">{description}</p>

        <ul className="flex flex-col gap-3 pt-2">
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
