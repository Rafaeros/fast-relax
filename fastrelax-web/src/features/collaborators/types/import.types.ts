import type { CredentialDelivery } from "@/features/authentication/types/auth.types";

/** Espelha `ImportRowErrorDTO`. */
export type ImportRowError = {
  /** Número da linha como aparece no Excel. */
  row: number;
  name: string | null;
  reason: string;
};

/**
 * Espelha `ImportedCredentialDTO`.
 *
 * Aparece só nesta resposta: o banco guarda apenas o hash, então o RH precisa
 * distribuir estas senhas antes de fechar a tela. O CPF vem mascarado pelo
 * backend — a lista é feita para ser exibida e impressa.
 */
export type ImportedCredential = {
  name: string;
  cpf: string;
  delivery: CredentialDelivery;
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
  /** Senhas dos colaboradores criados nesta importação, exibidas uma única vez. */
  credentials: ImportedCredential[];
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
