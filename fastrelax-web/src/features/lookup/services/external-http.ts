import { apiFailure, type ApiResult } from "@/lib/api/api.types";

/**
 * Cliente HTTP das bases públicas (Receita, Correios).
 *
 * <p>
 * Separado do `apiFetch` de propósito: aquele desempacota o `ApiResponseDTO` do
 * fastrelax-api e trata 401 como sessão expirada. Serviço de terceiro não tem
 * envelope nosso, não tem token e falha de um jeito diferente — cair fora do ar
 * é rotina, e o cadastro precisa continuar funcionando na mão quando isso
 * acontece.
 */

/**
 * Consulta externa é conveniência: passar disso, é mais rápido digitar.
 * Sem o limite, uma base lenta prenderia a Server Action até o timeout do Next.
 */
const TIMEOUT_MS = 6000;

const UNREACHABLE =
  "A consulta automática não respondeu. Preencha os campos manualmente.";

export type ExternalFetchOptions = {
  /**
   * Segundos de cache. Dado de CEP e de CNPJ muda em escala de meses, e a
   * mesma empresa costuma ser consultada várias vezes até o cadastro fechar.
   */
  revalidate?: number;
  /** Mensagem para 404 — cada base chama de um jeito o "não existe". */
  notFoundMessage?: string;
};

/**
 * Busca JSON de uma base pública. Nunca lança: rede, timeout e corpo inválido
 * viram a mesma falha que o chamador já sabe tratar.
 */
export async function fetchExternalJson<T>(
  url: string,
  options: ExternalFetchOptions = {},
): Promise<ApiResult<T>> {
  const { revalidate = 60 * 60 * 24, notFoundMessage } = options;

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), TIMEOUT_MS);

  try {
    const response = await fetch(url, {
      headers: { Accept: "application/json" },
      signal: controller.signal,
      next: { revalidate },
    });

    if (response.status === 404) {
      return apiFailure(notFoundMessage ?? "Nada encontrado para essa consulta.", 404);
    }

    // As bases gratuitas limitam por IP. Rodando em Server Action, o IP é o do
    // servidor e o limite é compartilhado por todo mundo — daí a mensagem
    // dizer o que fazer em vez de só "erro".
    if (response.status === 429) {
      return apiFailure(
        "Limite de consultas atingido. Tente de novo em instantes ou preencha manualmente.",
        429,
      );
    }

    if (!response.ok) {
      return apiFailure(UNREACHABLE, response.status);
    }

    return { ok: true, data: (await response.json()) as T, message: "" };
  } catch {
    // Inclui o abort do timeout: para quem preenche o formulário, base fora do
    // ar e base lenta demais são o mesmo problema.
    return apiFailure(UNREACHABLE, 0);
  } finally {
    clearTimeout(timeout);
  }
}
