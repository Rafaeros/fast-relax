"use client";

import { useState, useTransition, type FormEvent } from "react";
import { Button, Card, CardTitle, Icon, Input, useToast } from "@/components/ui";
import type { IconName } from "@/components/ui";
import { formatLongDate } from "@/lib/format";
import { updateSessionSettingsAction } from "@/features/settings/actions/session-settings.actions";
import {
  SESSION_SETTINGS_INITIAL_STATE,
  SETTINGS_LIMITS,
  type SessionSettings,
  type SessionSettingsField,
} from "@/features/settings/types/session-settings.types";
import { hasFieldErrors } from "@/lib/forms";

export type SessionSettingsFormProps = {
  settings: SessionSettings;
  /** Recebe a configuração salva — os cartões de resumo atualizam por aqui. */
  onSaved: (settings: SessionSettings) => void;
};

const FIELDS: {
  name: SessionSettingsField;
  label: string;
  hint: string;
  suffix: string;
  icon: IconName;
}[] = [
  {
    name: "defaultDurationMinutes",
    label: "Duração padrão da sessão",
    hint: "Tempo de cada agendamento.",
    suffix: "min",
    icon: "clock",
  },
  {
    name: "startGraceMinutes",
    label: "Tolerância de início",
    hint: "Quanto tempo após o horário o colaborador ainda pode iniciar. Zero expira na hora.",
    suffix: "min",
    icon: "bell",
  },
  {
    name: "earlyStartMinutes",
    label: "Antecedência de início",
    hint: "Quanto tempo antes do horário o colaborador já pode iniciar, se chegar adiantado.",
    suffix: "min",
    icon: "play",
  },
  {
    name: "maxAdvanceDays",
    label: "Antecedência máxima",
    hint: "Até quantos dias à frente é possível reservar.",
    suffix: "dias",
    icon: "calendar",
  },
];

export function SessionSettingsForm({ settings, onSaved }: SessionSettingsFormProps) {
  const [state, setState] = useState(SESSION_SETTINGS_INITIAL_STATE);
  const [pending, startTransition] = useTransition();
  const toast = useToast();

  const fieldErrors = state.fieldErrors ?? {};

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);

    startTransition(async () => {
      const result = await updateSessionSettingsAction(state, formData);
      setState(result);

      if (result.status === "success") {
        if (result.settings) onSaved(result.settings);
        if (result.message) toast.success(result.message);
        return;
      }

      // Limite fora da faixa aparece no próprio campo; o resto vem do servidor
      // e vai por toast.
      if (result.message && !hasFieldErrors(result.fieldErrors)) toast.error(result.message);
    });
  };

  return (
    <Card padding="lg" className="max-w-2xl">
      <form onSubmit={handleSubmit} className="flex flex-col gap-6" noValidate>
        <CardTitle>Parâmetros da agenda</CardTitle>

        <div className="flex flex-col gap-5">
          {FIELDS.map((field) => {
            const { min, max } = SETTINGS_LIMITS[field.name];

            return (
              <Input
                // A key muda com o valor vigente para o campo remontar já com o
                // que foi salvo, sem manter o rascunho da tentativa anterior.
                key={`${field.name}-${settings[field.name]}`}
                name={field.name}
                type="number"
                inputMode="numeric"
                label={field.label}
                hint={`${field.hint} Entre ${min} e ${max}.`}
                defaultValue={settings[field.name]}
                min={min}
                max={max}
                step={1}
                disabled={pending}
                error={fieldErrors[field.name]}
                leadingIcon={<Icon name={field.icon} />}
                trailing={
                  <span className="px-2.5 text-xs text-ink-tertiary">{field.suffix}</span>
                }
              />
            );
          })}
        </div>

        <div className="flex items-center justify-between gap-4 border-t border-line pt-5">
          <span className="text-xs text-ink-tertiary">
            {settings.updatedAt
              ? `Última alteração em ${formatLongDate(settings.updatedAt)}`
              : "Ainda sem alterações registradas"}
          </span>

          <Button
            type="submit"
            size="sm"
            disabled={pending}
            trailingIcon={
              pending ? <Icon name="loader" className="h-4 w-4 animate-spin" /> : undefined
            }
          >
            {pending ? "Salvando..." : "Salvar alterações"}
          </Button>
        </div>
      </form>
    </Card>
  );
}
