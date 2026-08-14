/** Espelha `ImportRowErrorDTO`. */
export type ImportRowError = {
  /** Número da linha como aparece no Excel. */
  row: number;
  name: string | null;
  reason: string;
};

/** Espelha `ImportResultDTO`. */
export type ImportResult = {
  totalRows: number;
  processed: number;
  failed: number;
  departmentsCreated: number;
  collaboratorsCreated: number;
  collaboratorsUpdated: number;
  schedulesSaved: number;
  errors: ImportRowError[];
};

export type ImportFormState = {
  status: "idle" | "success" | "error";
  message?: string;
  /** Presente quando o arquivo foi processado, mesmo com linhas recusadas. */
  result?: ImportResult;
};

export const IMPORT_INITIAL_STATE: ImportFormState = { status: "idle" };

/** Extensões aceitas pelo leitor de planilha do backend. */
export const IMPORT_ACCEPT = ".xlsx";
