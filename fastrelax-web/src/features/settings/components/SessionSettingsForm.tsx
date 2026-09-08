"use client";

import { useState, useTransition, type FormEvent } from "react";
import { Button, Card, CardTitle, Icon, Input, Select, useToast } from "@/components/ui";
import type { IconName } from "@/components/ui";
import { formatLongDate } from "@/lib/format";
import { updateSessionSettingsAction } from "@/features/settings/actions/session-settings.actions";
import {
  QUOTA_PERIOD_LABELS,
  QUOTA_PERIOD_OPTIONS,
  SESSION_SETTINGS_INITIAL_STATE,
  SETTINGS_LIMITS,
  type SessionQuotaPeriod,
  type SessionSettings,
  type SessionSettingsFieldErrors,
  type SessionSettingsNumericField,
} from "@/features/settings/types/session-settings.types";
import { hasFieldErrors } from "@/lib/forms";

export type SessionSettingsFormProps = {
  settings: SessionSettings;
  /** Recebe a configuração salva — os cartões de resumo atualizam por aqui. */
  onSaved: (settings: SessionSettings) => void;
};

const FIELDS: {
  name: SessionSettingsNumericField;
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
    name: "maxAdvanceDays",
    label: "Antecedência máxima",
    hint: "Até quantos dias à frente é possível reservar.",
    suffix: "dias",
    icon: "calendar",
  },
  {
    name: "stabilizationMinutes",
    label: "Estabilização da cadeira",
    hint: "Intervalo mínimo entre o fim de uma sessão e o início da próxima na mesma cadeira.",
    suffix: "min",
    icon: "chair",
  },
];

/**
 * Limite e período da cota, com a frase que os dois formam.
 *
 * <p>
 * Separado para poder ser controlado sem controlar o formulário inteiro: a
 * frase — "1 massagem por semana" — é o que torna a combinação legível, e ela
 * precisa mudar enquanto a pessoa escolhe, não depois de salvar.
 */
function QuotaFields({
  settings,
  disabled,
  fieldErrors,
}: {
  settings: SessionSettings;
  disabled: boolean;
  fieldErrors: SessionSettingsFieldErrors;
}) {
  const [limit, setLimit] = useState(String(settings.sessionQuotaLimit));
  const [period, setPeriod] = useState<SessionQuotaPeriod>(settings.sessionQuotaPeriod);

  const { min, max } = SETTINGS_LIMITS.sessionQuotaLimit;
  const count = Number(limit);
  const noun = count === 1 ? "massagem" : "massagens";

  return (
    <>
      <div className="grid gap-5 sm:grid-cols-[8rem_minmax(0,1fr)]">
        <Input
          name="sessionQuotaLimit"
          type="number"
          inputMode="numeric"
          label="Limite"
          value={limit}
          onChange={(event) => setLimit(event.target.value)}
          min={min}
          max={max}
          step={1}
          disabled={disabled}
          error={fieldErrors.sessionQuotaLimit}
          leadingIcon={<Icon name="heart" />}
        />

        <Select
          name="sessionQuotaPeriod"
          label="Contadas"
          value={period}
          onChange={(event) => setPeriod(event.target.value as SessionQuotaPeriod)}
          disabled={disabled}
          error={fieldErrors.sessionQuotaPeriod}
          options={QUOTA_PERIOD_OPTIONS}
        />
      </div>

      <p className="rounded-control bg-surface-hover px-4 py-3 text-xs text-ink-secondary">
        Cada colaborador poderá ter{" "}
        <strong className="text-ink-primary">
          {Number.isNaN(count) ? "—" : count} {noun} {QUOTA_PERIOD_LABELS[period]}
        </strong>
        .{" "}
        {period === "ACTIVE"
          ? "Não olha calendário: vale o que está marcado ou em andamento agora."
          : "Conta pela data da massagem. Cancelada e não comparecida não ocupam a cota."}
      </p>
    </>
  );
}

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

        <div className="flex flex-col gap-5 border-t border-line pt-6">
          <div className="flex flex-col gap-1">
            <CardTitle>Cota por colaborador</CardTitle>
            <p className="text-xs text-ink-tertiary">
              Quantas massagens cada pessoa pode ter. O teto da empresa continua sendo o
              número de cadeiras.
            </p>
          </div>

          {/* A key remonta os campos com o que foi salvo, descartando o
              rascunho da tentativa anterior — mesmo motivo dos campos acima. */}
          <QuotaFields
            key={`${settings.sessionQuotaLimit}-${settings.sessionQuotaPeriod}`}
            settings={settings}
            disabled={pending}
            fieldErrors={fieldErrors}
          />
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
