import type { LoginCredentials, LoginFieldErrors } from "@/features/authentication/types/auth.types";

export type ValidationResult =
  | { valid: true; data: LoginCredentials }
  | { valid: false; fieldErrors: LoginFieldErrors };

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

/**
 * Validação de entrada espelhando as constraints do `LoginRequestDTO`.
 * Roda antes da chamada de rede para dar retorno imediato — o backend continua
 * sendo a autoridade final.
 */
export function validateLoginInput(input: {
  email: string;
  password: string;
}): ValidationResult {
  const email = input.email.trim().toLowerCase();
  const password = input.password;
  const fieldErrors: LoginFieldErrors = {};

  if (!email) {
    fieldErrors.email = "Informe seu e-mail.";
  } else if (!EMAIL_PATTERN.test(email)) {
    fieldErrors.email = "E-mail inválido.";
  }

  if (!password) {
    fieldErrors.password = "Informe sua senha.";
  }

  if (Object.keys(fieldErrors).length > 0) {
    return { valid: false, fieldErrors };
  }

  return { valid: true, data: { email, password } };
}
