import {
  SESSION_QUOTA_PERIODS,
  SETTINGS_LIMITS,
  type SessionQuotaPeriod,
  type SessionSettingsField,
  type SessionSettingsFieldErrors,
  type SessionSettingsNumericField,
  type UpdateSessionSettingsInput,
} from "@/features/settings/types/session-settings.types";

export type SessionSettingsValidation =
  | { valid: true; data: UpdateSessionSettingsInput }
  | { valid: false; fieldErrors: SessionSettingsFieldErrors };

const LABELS: Record<SessionSettingsNumericField, string> = {
  defaultDurationMinutes: "A duração",
  startGraceMinutes: "A tolerância",
  maxAdvanceDays: "A antecedência",
  stabilizationMinutes: "A estabilização",
  sessionQuotaLimit: "O limite",
};

const UNITS: Record<SessionSettingsNumericField, string> = {
  defaultDurationMinutes: "minutos",
  startGraceMinutes: "minutos",
  maxAdvanceDays: "dias",
  stabilizationMinutes: "minutos",
  sessionQuotaLimit: "massagens",
};

/** Espelha as constraints de `UpdateSessionSettingsRequestDTO`. */
export function validateSessionSettingsInput(
  input: Record<SessionSettingsField, string>,
): SessionSettingsValidation {
  const fieldErrors: SessionSettingsFieldErrors = {};
  const parsed = {} as UpdateSessionSettingsInput;

  for (const field of Object.keys(SETTINGS_LIMITS) as SessionSettingsNumericField[]) {
    const raw = input[field]?.trim() ?? "";
    const value = Number(raw);
    const { min, max } = SETTINGS_LIMITS[field];

    if (!raw || Number.isNaN(value) || !Number.isInteger(value)) {
      fieldErrors[field] = "Informe um número inteiro.";
      continue;
    }

    if (value < min || value > max) {
      fieldErrors[field] = `${LABELS[field]} deve ficar entre ${min} e ${max} ${UNITS[field]}.`;
      continue;
    }

    parsed[field] = value;
  }

  // Valor fora da lista só chega aqui por request forjado — o campo é um
  // `<select>`. Recusar mesmo assim evita mandar ao backend um enum que ele
  // devolveria como erro de desserialização, sem campo marcado na tela.
  const period = input.sessionQuotaPeriod?.trim() as SessionQuotaPeriod;
  if (!SESSION_QUOTA_PERIODS.includes(period)) {
    fieldErrors.sessionQuotaPeriod = "Escolha um período válido.";
  } else {
    parsed.sessionQuotaPeriod = period;
  }

  if (Object.keys(fieldErrors).length > 0) {
    return { valid: false as const, fieldErrors };
  }

  return { valid: true as const, data: parsed };
}

/** Converte os erros do backend (`"campo: mensagem"`) em erro por campo. */
export function mapSessionSettingsApiErrors(errors: string[]): SessionSettingsFieldErrors {
  const fieldErrors: SessionSettingsFieldErrors = {};
  const fields: SessionSettingsField[] = [
    ...(Object.keys(SETTINGS_LIMITS) as SessionSettingsNumericField[]),
    "sessionQuotaPeriod",
  ];

  for (const entry of errors) {
    const [rawField, ...rest] = entry.split(":");
    const message = rest.join(":").trim();
    const field = fields.find((candidate) => candidate === rawField.trim());

    if (field && message) {
      fieldErrors[field] = message;
    }
  }

  return fieldErrors;
}
