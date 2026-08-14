/**
 * Envelope padrão do fastrelax-api (`ApiResponseDTO`).
 * Campos nulos são omitidos pelo backend (`@JsonInclude(NON_NULL)`).
 */
export type ApiEnvelope<T> = {
  status: "success" | "warning" | "error";
  message: string;
  data?: T | null;
  warnings?: string[] | null;
  errors?: string[] | null;
  timestamp: string;
};

/** Resultado já desempacotado — nunca lança, sempre discriminado por `ok`. */
export type ApiResult<T> =
  | { ok: true; data: T; message: string }
  | { ok: false; message: string; errors: string[]; status: number };

export function apiFailure(
  message: string,
  status: number,
  errors: string[] = [],
): Extract<ApiResult<never>, { ok: false }> {
  return { ok: false, message, errors, status };
}
