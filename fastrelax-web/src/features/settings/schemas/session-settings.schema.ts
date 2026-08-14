import {
  SETTINGS_LIMITS,
  type SessionSettingsField,
  type SessionSettingsFieldErrors,
  type UpdateSessionSettingsInput,
} from "@/features/settings/types/session-settings.types";

export type SessionSettingsValidation =
  | { valid: true; data: UpdateSessionSettingsInput }
  | { valid: false; fieldErrors: SessionSettingsFieldErrors };

const LABELS: Record<SessionSettingsField, string> = {
  defaultDurationMinutes: "A duração",
  startGraceMinutes: "A tolerância",
  maxAdvanceDays: "A antecedência",
};

const UNITS: Record<SessionSettingsField, string> = {
  defaultDurationMinutes: "minutos",
  startGraceMinutes: "minutos",
  maxAdvanceDays: "dias",
};

/** Espelha as constraints de `UpdateSessionSettingsRequestDTO`. */
export function validateSessionSettingsInput(input: Record<SessionSettingsField, string>) {
  const fieldErrors: SessionSettingsFieldErrors = {};
  const parsed = {} as UpdateSessionSettingsInput;

  for (const field of Object.keys(SETTINGS_LIMITS) as SessionSettingsField[]) {
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

  if (Object.keys(fieldErrors).length > 0) {
    return { valid: false as const, fieldErrors };
  }

  return { valid: true as const, data: parsed };
}

/** Converte os erros do backend (`"campo: mensagem"`) em erro por campo. */
export function mapSessionSettingsApiErrors(errors: string[]): SessionSettingsFieldErrors {
  const fieldErrors: SessionSettingsFieldErrors = {};
  const fields = Object.keys(SETTINGS_LIMITS) as SessionSettingsField[];

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
