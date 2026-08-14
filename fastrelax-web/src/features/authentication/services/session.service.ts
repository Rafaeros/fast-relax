import { cookies } from "next/headers";
import { env } from "@/lib/env";
import type { AuthSession } from "@/features/authentication/types/auth.types";

/**
 * Sessão em cookies httpOnly: o JWT nunca fica acessível a JavaScript do
 * cliente, o que elimina o vetor clássico de roubo de token por XSS.
 */

const ACCESS_TOKEN_COOKIE = "fastrelax.token";
const REFRESH_TOKEN_COOKIE = "fastrelax.refresh";

/** O refresh vale 30 dias no backend (`api.security.refresh-token.expiration-days`). */
const REFRESH_MAX_AGE_SECONDS = 60 * 60 * 24 * 30;

const BASE_COOKIE_OPTIONS = {
  httpOnly: true,
  sameSite: "lax",
  secure: env.isProduction,
  path: "/",
} as const;

export async function createSession(session: AuthSession): Promise<void> {
  const cookieStore = await cookies();

  cookieStore.set(ACCESS_TOKEN_COOKIE, session.token, {
    ...BASE_COOKIE_OPTIONS,
    maxAge: session.expiresInSeconds,
  });

  cookieStore.set(REFRESH_TOKEN_COOKIE, session.refreshToken, {
    ...BASE_COOKIE_OPTIONS,
    maxAge: REFRESH_MAX_AGE_SECONDS,
  });
}

export async function destroySession(): Promise<void> {
  const cookieStore = await cookies();
  cookieStore.delete(ACCESS_TOKEN_COOKIE);
  cookieStore.delete(REFRESH_TOKEN_COOKIE);
}

export async function readAccessToken(): Promise<string | null> {
  const cookieStore = await cookies();
  return cookieStore.get(ACCESS_TOKEN_COOKIE)?.value ?? null;
}

export async function readRefreshToken(): Promise<string | null> {
  const cookieStore = await cookies();
  return cookieStore.get(REFRESH_TOKEN_COOKIE)?.value ?? null;
}
