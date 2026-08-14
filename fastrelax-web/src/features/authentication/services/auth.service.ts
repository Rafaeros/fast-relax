import { cache } from "react";
import { apiFetch } from "@/lib/api/http";
import type { ApiResult } from "@/lib/api/api.types";
import { readAccessToken } from "@/features/authentication/services/session.service";
import type {
  AuthSession,
  AuthUser,
  LoginCredentials,
} from "@/features/authentication/types/auth.types";

/**
 * Camada de acesso à API de autenticação. Só fala HTTP — cookies e navegação
 * ficam nas actions, o que mantém este módulo testável e reutilizável.
 */

export function signIn(credentials: LoginCredentials): Promise<ApiResult<AuthSession>> {
  return apiFetch<AuthSession>("/auth/login", {
    method: "POST",
    body: credentials,
  });
}

export function signOut(refreshToken: string): Promise<ApiResult<null>> {
  return apiFetch<null>("/auth/logout", {
    method: "POST",
    body: { refreshToken },
  });
}

export function refreshSession(refreshToken: string): Promise<ApiResult<AuthSession>> {
  return apiFetch<AuthSession>("/auth/refresh", {
    method: "POST",
    body: { refreshToken },
  });
}

/**
 * Usuário autenticado ou `null`.
 * `cache` deduplica a chamada dentro da mesma request — layout e página podem
 * pedir o usuário sem gerar duas idas à API.
 */
export const getCurrentUser = cache(async (): Promise<AuthUser | null> => {
  const token = await readAccessToken();
  if (!token) return null;

  const result = await apiFetch<AuthUser>("/users/me", { token });
  return result.ok ? result.data : null;
});
