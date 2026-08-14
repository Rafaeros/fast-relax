/** Espelha `SessionSettingsResponseDTO`. */
export type SessionSettings = {
  defaultDurationMinutes: number;
  startGraceMinutes: number;
  maxAdvanceDays: number;
  updatedAt: string | null;
};

/** Espelha `UpdateSessionSettingsRequestDTO`. */
export type UpdateSessionSettingsInput = {
  defaultDurationMinutes: number;
  startGraceMinutes: number;
  maxAdvanceDays: number;
};

export type SessionSettingsField = keyof UpdateSessionSettingsInput;

/**
 * Limites do DTO, que por sua vez espelham o CHECK da tabela.
 * Ficam aqui para input e validação não divergirem do backend.
 */
export const SETTINGS_LIMITS: Record<SessionSettingsField, { min: number; max: number }> = {
  defaultDurationMinutes: { min: 1, max: 120 },
  startGraceMinutes: { min: 0, max: 60 },
  maxAdvanceDays: { min: 1, max: 365 },
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

/** Padrões usados só quando a leitura falha, para o formulário renderizar. */
export function fallbackSessionSettings(): SessionSettings {
  return {
    defaultDurationMinutes: 15,
    startGraceMinutes: 5,
    maxAdvanceDays: 7,
    updatedAt: null,
  };
}
