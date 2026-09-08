/** Espelha o enum `SessionQuotaPeriod` do fastrelax-api. */
export const SESSION_QUOTA_PERIODS = ["ACTIVE", "DAY", "WEEK", "MONTH"] as const;

export type SessionQuotaPeriod = (typeof SESSION_QUOTA_PERIODS)[number];

/**
 * Como cada período aparece na tela.
 *
 * <p>
 * `ACTIVE` é o único que não olha calendário: conta o que está marcado agora.
 * Com limite 1 é a regra que o sistema tinha fixa — uma massagem por vez.
 */
export const QUOTA_PERIOD_LABELS: Record<SessionQuotaPeriod, string> = {
  ACTIVE: "marcadas ao mesmo tempo",
  DAY: "por dia",
  WEEK: "por semana",
  MONTH: "por mês",
};

/** Texto do `<select>` — mais explícito que o rótulo curto usado nas frases. */
export const QUOTA_PERIOD_OPTIONS: { value: SessionQuotaPeriod; label: string }[] = [
  { value: "ACTIVE", label: "Marcadas ao mesmo tempo" },
  { value: "DAY", label: "Por dia" },
  { value: "WEEK", label: "Por semana (segunda a domingo)" },
  { value: "MONTH", label: "Por mês" },
];

/** Espelha `SessionSettingsResponseDTO`. */
export type SessionSettings = {
  defaultDurationMinutes: number;
  startGraceMinutes: number;
  maxAdvanceDays: number;
  stabilizationMinutes: number;
  sessionQuotaLimit: number;
  sessionQuotaPeriod: SessionQuotaPeriod;
  /** Mesmo período em português, enviado pela API para exibição. */
  sessionQuotaPeriodLabel: string;
  updatedAt: string | null;
};

/** Espelha `UpdateSessionSettingsRequestDTO`. */
export type UpdateSessionSettingsInput = {
  defaultDurationMinutes: number;
  startGraceMinutes: number;
  maxAdvanceDays: number;
  stabilizationMinutes: number;
  sessionQuotaLimit: number;
  sessionQuotaPeriod: SessionQuotaPeriod;
};

export type SessionSettingsField = keyof UpdateSessionSettingsInput;

/** O período é escolha em lista; todo o resto do formulário é número. */
export type SessionSettingsNumericField = Exclude<SessionSettingsField, "sessionQuotaPeriod">;

/**
 * Limites do DTO, que por sua vez espelham o CHECK da tabela.
 * Ficam aqui para input e validação não divergirem do backend.
 */
export const SETTINGS_LIMITS: Record<SessionSettingsNumericField, { min: number; max: number }> = {
  defaultDurationMinutes: { min: 1, max: 120 },
  startGraceMinutes: { min: 0, max: 60 },
  maxAdvanceDays: { min: 1, max: 365 },
  stabilizationMinutes: { min: 0, max: 30 },
  sessionQuotaLimit: { min: 1, max: 20 },
};

export type SessionSettingsFieldErrors = Partial<Record<SessionSettingsField, string>>;

export type SessionSettingsFormState = {
  status: "idle" | "success" | "error";
  message?: string;
  fieldErrors?: SessionSettingsFieldErrors;
  /** Configuração vigente após salvar — alimenta os cartões de resumo. */
  settings?: SessionSettings;
};

export const SESSION_SETTINGS_INITIAL_STATE: SessionSettingsFormState = { status: "idle" };

/** Frase pronta da cota, para cartão e resumo dizerem a mesma coisa. */
export function describeQuota(settings: SessionSettings): string {
  const { sessionQuotaLimit: limit, sessionQuotaPeriod: period } = settings;
  const noun = limit === 1 ? "massagem" : "massagens";
  return `${limit} ${noun} ${QUOTA_PERIOD_LABELS[period]}`;
}

/** Padrões usados só quando a leitura falha, para o formulário renderizar. */
export function fallbackSessionSettings(): SessionSettings {
  return {
    defaultDurationMinutes: 15,
    startGraceMinutes: 5,
    maxAdvanceDays: 7,
    stabilizationMinutes: 1,
    sessionQuotaLimit: 1,
    sessionQuotaPeriod: "ACTIVE",
    sessionQuotaPeriodLabel: QUOTA_PERIOD_LABELS.ACTIVE,
    updatedAt: null,
  };
}
