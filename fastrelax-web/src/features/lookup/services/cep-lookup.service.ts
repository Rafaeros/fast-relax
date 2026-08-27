import { onlyDigits } from "@/lib/format";
import { apiFailure, type ApiResult } from "@/lib/api/api.types";
import { fetchExternalJson } from "@/features/lookup/services/external-http";
import type { CepLookup } from "@/features/lookup/types/lookup.types";

/**
 * Consulta de CEP no ViaCEP.
 *
 * <p>
 * Aberto e sem chave. Devolve logradouro, bairro, município e UF — o número e o
 * complemento continuam com quem preenche, porque o CEP não os conhece.
 */

const BASE_URL = "https://viacep.com.br/ws";

/** Recorte do payload do ViaCEP — só o que o cadastro aproveita. */
type ViaCepAddress = {
  cep?: string;
  logradouro?: string;
  complemento?: string;
  bairro?: string;
  localidade?: string;
  uf?: string;
  ibge?: string;
  /** Presente só quando o CEP não existe; o HTTP continua 200. */
  erro?: boolean | string;
};

export async function lookupCep(cep: string): Promise<ApiResult<CepLookup>> {
  const digits = onlyDigits(cep);

  if (digits.length !== 8) {
    return apiFailure("Informe os 8 dígitos do CEP para consultar.", 400);
  }

  const result = await fetchExternalJson<ViaCepAddress>(`${BASE_URL}/${digits}/json/`, {
    notFoundMessage: "CEP não encontrado.",
  });

  if (!result.ok) return result;

  // O ViaCEP responde 200 com `{"erro": true}` para CEP inexistente. Sem esta
  // checagem, o formulário seria preenchido com um endereço todo vazio — pior
  // que não consultar, porque apagaria o que já estava digitado.
  if (result.data.erro) {
    return apiFailure("CEP não encontrado.", 404);
  }

  return {
    ok: true,
    data: toCepLookup(digits, result.data),
    message: "Endereço encontrado.",
  };
}

function toCepLookup(digits: string, payload: ViaCepAddress): CepLookup {
  return {
    address: {
      cep: onlyDigits(payload.cep ?? digits),
      street: payload.logradouro?.trim() ?? "",
      // O CEP não tem número; o campo existe para o endereço ter uma forma só.
      number: "",
      complement: payload.complemento?.trim() ?? "",
      district: payload.bairro?.trim() ?? "",
      cityName: payload.localidade?.trim() ?? "",
      stateAcronym: payload.uf?.trim().toUpperCase() ?? "",
      ibgeCode: onlyDigits(payload.ibge ?? ""),
    },
  };
}
