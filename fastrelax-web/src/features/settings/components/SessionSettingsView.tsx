"use client";

import { useState } from "react";
import { Alert } from "@/components/ui";
import { SessionSettingsForm } from "@/features/settings/components/SessionSettingsForm";
import { SessionSettingsSummary } from "@/features/settings/components/SessionSettingsSummary";
import type { SessionSettings } from "@/features/settings/types/session-settings.types";

export type SessionSettingsViewProps = {
  initialSettings: SessionSettings;
  /** Mensagem da API quando a leitura falhou no servidor. */
  initialError?: string;
};

/**
 * Une resumo e formulário sob o mesmo estado: salvar atualiza os cartões na
 * hora, sem recarregar a página.
 */
export function SessionSettingsView({
  initialSettings,
  initialError,
}: SessionSettingsViewProps) {
  const [settings, setSettings] = useState(initialSettings);

  return (
    <div className="flex flex-col gap-6">
      {initialError && (
        <Alert tone="error" title="Não foi possível carregar as configurações">
          {initialError} Os valores abaixo são os padrões do sistema — salvar vai gravá-los.
        </Alert>
      )}

      <SessionSettingsSummary settings={settings} />
      <SessionSettingsForm settings={settings} onSaved={setSettings} />
    </div>
  );
}
