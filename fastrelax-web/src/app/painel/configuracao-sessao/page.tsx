import type { Metadata } from "next";
import { SessionSettingsView } from "@/features/settings/components/SessionSettingsView";
import { getSessionSettings } from "@/features/settings/services/session-settings.service";
import { fallbackSessionSettings } from "@/features/settings/types/session-settings.types";

export const metadata: Metadata = {
  title: "Configuração da sessão — physical",
};

export default async function ConfiguracaoSessaoPage() {
  const result = await getSessionSettings();

  return (
    <SessionSettingsView
      initialSettings={result.ok ? result.data : fallbackSessionSettings()}
      initialError={result.ok ? undefined : result.message}
    />
  );
}
