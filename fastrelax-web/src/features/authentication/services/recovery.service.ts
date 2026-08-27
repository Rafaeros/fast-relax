import { apiFetch } from "@/lib/api/http";
import type { ApiResult } from "@/lib/api/api.types";
import type { RecoveryTarget } from "@/features/authentication/types/auth.types";

/**
 * Recuperação de senha e convite de primeiro acesso.
 *
 * <p>
 * Nenhuma destas chamadas leva token de sessão: quem chega aqui perdeu o acesso,
 * ou nunca teve. A autorização é o próprio link recebido por e-mail.
 */

const RESOURCE = "/auth/password";

/** Painel: o e-mail é único no sistema inteiro. */
export function requestUserReset(email: string): Promise<ApiResult<null>> {
  return apiFetch<null>(`${RESOURCE}/forgot`, {
    method: "POST",
    body: { email },
  });
}

/**
 * Colaborador: o e-mail só é único dentro da empresa, então o CNPJ é
 * obrigatório. É o mesmo que ele já digita no login.
 */
export function requestCollaboratorReset(cnpj: string, email: string): Promise<ApiResult<null>> {
  return apiFetch<null>(`${RESOURCE}/collaborator/forgot`, {
    method: "POST",
    body: { cnpj, email },
  });
}

/**
 * Valida o link sem gastá-lo.
 *
 * <p>
 * A API responde 200 mesmo com token inválido, e sinaliza pelo corpo nulo —
 * link expirado não é erro de servidor, é um estado que a tela precisa desenhar.
 */
export function describeToken(token: string): Promise<ApiResult<RecoveryTarget | null>> {
  return apiFetch<RecoveryTarget | null>(
    `${RESOURCE}/token?token=${encodeURIComponent(token)}`,
  );
}

export function resetPasswordWithToken(
  token: string,
  newPassword: string,
  confirmNewPassword: string,
): Promise<ApiResult<null>> {
  return apiFetch<null>(`${RESOURCE}/reset`, {
    method: "POST",
    body: { token, newPassword, confirmNewPassword },
  });
}
