import type { PageParams } from "@/lib/api/pagination.types";

/** Espelha `CompanyResponseDTO` do fastrelax-api. */
export type Company = {
  id: number;
  /** Só dígitos — a máscara é aplicada na exibição. */
  cnpj: string;
  name: string;
  email: string;
  phone: string;
  active: boolean;
  addressId: number | null;
  cep: string | null;
  street: string | null;
  number: string | null;
  complement: string | null;
  cityId: number | null;
  cityName: string | null;
  stateAcronym: string | null;
  /**
   * SSID da rede em que as cadeiras desta empresa entram.
   *
   * A senha não vem junto, por decisão explícita: é segredo de terceiro sob
   * nossa guarda, e devolvê-la a espalharia por resposta HTTP, log de proxy e
   * histórico de navegador. Só o ESP32 recebe o valor, na hora de gravar.
   */
  wifiSsid: string | null;
  /** Se há senha gravada — o que a tela precisa saber sem receber o valor. */
  wifiConfigured: boolean;
  wifiUpdatedAt: string | null;
  createdAt: string;
};

/**
 * Filtro da listagem.
 * `GET /companies` aceita só paginação: estes campos são aplicados no cliente,
 * sobre as linhas já carregadas.
 */
export type CompanyFilter = {
  search?: string;
  active?: boolean;
};

export type ListCompaniesParams = PageParams;

/** Espelha `SaveAddressRequestDTO`. */
export type SaveAddressInput = {
  cityId: number;
  cep: string;
  street: string;
  number: string;
  complement?: string;
};

/** Espelha `SaveCompanyRequestDTO`, usado no cadastro e na edição. */
export type SaveCompanyInput = {
  cnpj: string;
  name: string;
  email: string;
  phone: string;
  address: SaveAddressInput;
  /** Rede das cadeiras. Opcional: a empresa é cadastrada antes do equipamento. */
  wifiSsid?: string;
  /** Em branco mantém a senha atual — não há como relê-la para reenviar. */
  wifiPassword?: string;
};

export type CompanyFieldErrors = Partial<
  Record<
    | "cnpj"
    | "name"
    | "email"
    | "phone"
    | "cityId"
    | "cep"
    | "street"
    | "number"
    | "wifiSsid",
    string
  >
>;

/** Estado dos formulários de cadastro e edição. */
export type CompanyFormState = {
  status: "idle" | "success" | "error";
  message?: string;
  fieldErrors?: CompanyFieldErrors;
};

export const COMPANY_INITIAL_STATE: CompanyFormState = { status: "idle" };
