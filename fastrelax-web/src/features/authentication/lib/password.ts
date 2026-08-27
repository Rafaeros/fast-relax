import type { PasswordFieldErrors } from "@/features/authentication/types/auth.types";

/**
 * Espelha as constraints de `FirstAccessPasswordRequestDTO` e
 * `ChangePasswordRequestDTO`.
 *
 * <p>
 * O backend continua sendo a autoridade — isto só evita uma ida à rede para
 * errar o óbvio, e mantém a mesma regra nos dois formulários.
 *
 * @returns os erros encontrados, ou `null` quando está tudo certo
 */
export function validateNewPassword(
  newPassword: string,
  confirmNewPassword: string,
): PasswordFieldErrors | null {
  const fieldErrors: PasswordFieldErrors = {};

  if (newPassword.length < 8 || newPassword.length > 100) {
    fieldErrors.newPassword = "A senha deve ter entre 8 e 100 caracteres.";
  }
  if (newPassword !== confirmNewPassword) {
    fieldErrors.confirmNewPassword = "A confirmação não confere com a nova senha.";
  }

  return Object.keys(fieldErrors).length > 0 ? fieldErrors : null;
}
