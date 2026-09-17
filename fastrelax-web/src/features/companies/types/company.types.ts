import type { PageParams } from "@/lib/api/pagination.types";

/** Espelha `CompanyResponseDTO` do fastrelax-api. */
export type Company = {
  id: number;
  /** Só dígitos — a máscara é aplicada na exibição. */
  cnpj: string;
  /** O que o colaborador digita na tela de login em vez do CNPJ. */
  slug: string;
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

/**
 * Espelha `SaveCompanyRequestDTO`, usado no cadastro e na edição pela
 * Physical. Sem rede: quem cadastra a rede das cadeiras é a própria empresa,
 * pelo `SaveWifiInput` abaixo — a Physical não vê nem edita esse valor.
 */
export type SaveCompanyInput = {
  cnpj: string;
  /** Em branco deriva da primeira palavra do nome. */
  slug?: string;
  name: string;
  email: string;
  phone: string;
  address: SaveAddressInput;
};

export type CompanyFieldErrors = Partial<
  Record<"cnpj" | "slug" | "name" | "email" | "phone" | "cityId" | "cep" | "street" | "number", string>
>;

/** Estado dos formulários de cadastro e edição. */
export type CompanyFormState = {
  status: "idle" | "success" | "error";
  message?: string;
  fieldErrors?: CompanyFieldErrors;
};

export const COMPANY_INITIAL_STATE: CompanyFormState = { status: "idle" };

/**
 * Espelha `SaveWifiRequestDTO`: autoatendimento do RH/gestor da empresa. A
 * Physical não cadastra nem enxerga — ela só aplica a rede ao dispositivo pelo
 * push de `/chairs/{id}/network`.
 */
export type SaveWifiInput = {
  /** Em branco apaga o Wi-Fi cadastrado (SSID e senha). */
  wifiSsid?: string;
  /** Em branco mantém a senha atual — não há como relê-la para reenviar. */
  wifiPassword?: string;
};

export type WifiFieldErrors = Partial<Record<"wifiSsid", string>>;

export type WifiFormState = {
  status: "idle" | "success" | "error";
  message?: string;
  fieldErrors?: WifiFieldErrors;
  /** Empresa atualizada — a tela usa para refletir o novo SSID sem recarregar. */
  company?: Company;
};

export const WIFI_INITIAL_STATE: WifiFormState = { status: "idle" };
