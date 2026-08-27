"use server";

import { lookupCep } from "@/features/lookup/services/cep-lookup.service";
import { lookupCnpj } from "@/features/lookup/services/cnpj-lookup.service";
import type { CepLookup, CnpjLookup } from "@/features/lookup/types/lookup.types";

/**
 * Ponte do formulário para as bases públicas.
 *
 * <p>
 * Server Action, e não `fetch` no cliente, por dois motivos: a resposta entra
 * no cache do Next (o mesmo CNPJ é consultado várias vezes até o cadastro
 * fechar) e a URL da base fica fora do bundle, o que permite trocá-la sem
 * mexer em componente.
 */

export type LookupResult<T> = { ok: true; data: T; message: string } | { ok: false; message: string };

export async function lookupCnpjAction(cnpj: string): Promise<LookupResult<CnpjLookup>> {
  return unwrap(await lookupCnpj(cnpj));
}

export async function lookupCepAction(cep: string): Promise<LookupResult<CepLookup>> {
  return unwrap(await lookupCep(cep));
}

/**
 * Descarta `status` e `errors` do `ApiResult`: a tela só decide entre preencher
 * os campos e mostrar um aviso, e o código HTTP de uma base de terceiro não
 * significa nada para ela.
 */
function unwrap<T>(result: { ok: true; data: T; message: string } | { ok: false; message: string }) {
  return result.ok
    ? { ok: true as const, data: result.data, message: result.message }
    : { ok: false as const, message: result.message };
}
