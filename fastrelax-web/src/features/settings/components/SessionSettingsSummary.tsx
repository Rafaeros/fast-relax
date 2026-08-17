"use client";

import { Card, Icon, Stat } from "@/components/ui";
import type { IconName } from "@/components/ui";
import type { SessionSettings } from "@/features/settings/types/session-settings.types";

export type SessionSettingsSummaryProps = {
  settings: SessionSettings;
};

/** Os parâmetros vigentes, em leitura rápida acima do formulário. */
export function SessionSettingsSummary({ settings }: SessionSettingsSummaryProps) {
  const cards: { value: string; label: string; icon: IconName }[] = [
    {
      value: `${settings.defaultDurationMinutes} min`,
      label: "Duração padrão",
      icon: "clock",
    },
    {
      value: `${settings.startGraceMinutes} min`,
      label: "Tolerância de início",
      icon: "bell",
    },
    {
      value: `${settings.earlyStartMinutes} min`,
      label: "Antecedência de início",
      icon: "play",
    },
    {
      value: `${settings.maxAdvanceDays} ${settings.maxAdvanceDays === 1 ? "dia" : "dias"}`,
      label: "Antecedência máxima",
      icon: "calendar",
    },
  ];

  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
      {cards.map((card) => (
        <Card key={card.label} padding="lg">
          <Stat value={card.value} label={card.label} icon={<Icon name={card.icon} />} />
        </Card>
      ))}
    </div>
  );
}
