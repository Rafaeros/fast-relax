import { onlyDigits } from "@/lib/format";
import { apiFailure, type ApiResult } from "@/lib/api/api.types";
import { fetchExternalJson } from "@/features/lookup/services/external-http";
import type { CnpjLookup } from "@/features/lookup/types/lookup.types";

/**
 * Consulta de CNPJ na base aberta do CNPJá (dados públicos da Receita Federal).
 *
 * <p>
 * Endpoint aberto: não pede chave, e por isso limita por IP (poucas consultas
 * por minuto). É conveniência de digitação, não fonte de verdade — o cadastro
 * continua editável depois de preenchido, e uma consulta que falha não impede
 * ninguém de cadastrar.
 */

const BASE_URL = "https://open.cnpja.com/office";

/** Situação cadastral 2 = "Ativa" no vocabulário da Receita. */
const STATUS_ACTIVE = 2;

/** Recorte do payload do CNPJá — só o que o cadastro aproveita. */
type CnpjaOffice = {
  taxId?: string;
  alias?: string | null;
  company?: { name?: string | null } | null;
  status?: { id?: number; text?: string } | null;
  address?: {
    zip?: string | null;
    street?: string | null;
    number?: string | null;
    details?: string | null;
    district?: string | null;
    city?: string | null;
    state?: string | null;
    municipality?: number | string | null;
  } | null;
  phones?: { area?: string; number?: string }[] | null;
  emails?: { address?: string }[] | null;
};

export async function lookupCnpj(cnpj: string): Promise<ApiResult<CnpjLookup>> {
  const digits = onlyDigits(cnpj);

  if (digits.length !== 14) {
    return apiFailure("Informe os 14 dígitos do CNPJ para consultar.", 400);
  }

  const result = await fetchExternalJson<CnpjaOffice>(`${BASE_URL}/${digits}`, {
    notFoundMessage: "CNPJ não encontrado na base da Receita.",
  });

  if (!result.ok) return result;

  return {
    ok: true,
    data: toCnpjLookup(digits, result.data),
    message: "Dados encontrados na Receita Federal.",
  };
}

function toCnpjLookup(digits: string, payload: CnpjaOffice): CnpjLookup {
  const address = payload.address ?? {};
  // Só o primeiro de cada: o cadastro tem um campo de telefone e um de e-mail,
  // e a Receita devolve o principal primeiro.
  const [phone] = payload.phones ?? [];
  const [email] = payload.emails ?? [];

  return {
    taxId: payload.taxId ?? digits,
    name: payload.company?.name?.trim() ?? "",
    alias: payload.alias?.trim() ?? "",
    status: payload.status?.text ?? "",
    active: payload.status?.id === STATUS_ACTIVE,
    email: email?.address?.trim().toLowerCase() ?? "",
    phone: onlyDigits(`${phone?.area ?? ""}${phone?.number ?? ""}`),
    address: {
      cep: onlyDigits(address.zip ?? ""),
      street: address.street?.trim() ?? "",
      number: address.number?.trim() ?? "",
      complement: address.details?.trim() ?? "",
      district: address.district?.trim() ?? "",
      cityName: address.city?.trim() ?? "",
      stateAcronym: address.state?.trim().toUpperCase() ?? "",
      ibgeCode: onlyDigits(String(address.municipality ?? "")),
    },
  };
}
