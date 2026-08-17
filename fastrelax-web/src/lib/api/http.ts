import { env } from "@/lib/env";
import { apiFailure, type ApiEnvelope, type ApiResult } from "@/lib/api/api.types";

export type ApiRequestOptions = {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: unknown;
  /** Bearer token; quando ausente a chamada sai anônima. */
  token?: string | null;
  /** Repassado ao fetch do Next. Padrão: sem cache (dados de sessão). */
  cache?: RequestCache;
  revalidate?: number;
  signal?: AbortSignal;
};

const GENERIC_ERROR =
  "Não foi possível falar com o servidor agora. Tente novamente em instantes.";

/**
 * Cliente HTTP único da aplicação.
 *
 * Desempacota o `ApiResponseDTO` do backend e normaliza qualquer falha
 * (HTTP, rede ou corpo inválido) no mesmo formato — quem chama nunca precisa
 * de try/catch nem conhecer o envelope.
 */
export async function apiFetch<T>(
  path: string,
  options: ApiRequestOptions = {},
): Promise<ApiResult<T>> {
  const { method = "GET", body, token, cache, revalidate, signal } = options;

  // Upload de arquivo: o browser precisa definir o Content-Type sozinho para
  // incluir o boundary do multipart — declarar JSON aqui quebraria o parse.
  const isMultipart = typeof FormData !== "undefined" && body instanceof FormData;

  let response: Response;

  try {
    response = await fetch(`${env.apiUrl}${path}`, {
      method,
      headers: {
        ...(isMultipart ? {} : { "Content-Type": "application/json" }),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: body === undefined ? undefined : isMultipart ? (body as FormData) : JSON.stringify(body),
      cache: cache ?? (revalidate === undefined ? "no-store" : undefined),
      next: revalidate === undefined ? undefined : { revalidate },
      signal,
    });
  } catch {
    // API fora do ar, DNS, timeout: a causa real vai para o log do servidor,
    // o usuário recebe uma mensagem acionável.
    return apiFailure(GENERIC_ERROR, 0);
  }

  const envelope = await readEnvelope<T>(response);

  if (!response.ok || envelope?.status === "error" || envelope?.status === "warning") {
    return apiFailure(
      envelope?.message ?? GENERIC_ERROR,
      response.status,
      envelope?.errors ?? [],
    );
  }

  // 204 é resposta válida e sem corpo: a API usa para "consultei e não há nada"
  // (ex.: colaborador sem sessão em aberto). Tratar como falha faria o app
  // mostrar erro onde a resposta correta é "nenhum".
  if (response.status === 204) {
    return { ok: true, data: null as T, message: "" };
  }

  if (!envelope) {
    return apiFailure(GENERIC_ERROR, response.status);
  }

  return {
    ok: true,
    // Endpoints de sucesso sem corpo (logout) devolvem `data` ausente.
    data: (envelope.data ?? null) as T,
    message: envelope.message,
  };
}

async function readEnvelope<T>(response: Response): Promise<ApiEnvelope<T> | null> {
  const text = await response.text();
  if (!text) return null;

  try {
    return JSON.parse(text) as ApiEnvelope<T>;
  } catch {
    return null;
  }
}
