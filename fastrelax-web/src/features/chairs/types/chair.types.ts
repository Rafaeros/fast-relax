import type { PageParams } from "@/lib/api/pagination.types";

/** Espelha `ChairResponseDTO` do fastrelax-api. */
export type Chair = {
  id: number;
  name: string;
  macAddress: string;
  ipAddress: string | null;
  port: number;
  /** Versão gravada no dispositivo; nula quando nunca passou pela atualização formal. */
  firmwareId: number | null;
  firmwareVersion: string | null;
  /**
   * Empresa dona do equipamento. Preenchido sempre, mas só a listagem do
   * SYSADMIN exibe: para quem opera dentro de uma empresa é sempre a própria.
   */
  companyId: number | null;
  companyName: string | null;
  /** Ponto de acesso fixado; nulo deixa o ESP32 escolher o de melhor sinal. */
  wifiBssid: string | null;
  /** Quando o ESP32 confirmou ter gravado a configuração de rede. */
  networkSyncedAt: string | null;
  /** SSID que o dispositivo relatou no último heartbeat. */
  reportedSsid: string | null;
  /**
   * O dispositivo está na rede que a empresa configurou.
   *
   * Diferente de `networkSyncedAt`: aquele diz que o envio foi aceito, este que
   * a cadeira de fato entrou na rede. Uma cadeira pode ter gravado o SSID novo
   * e continuar no antigo — e é essa a que some quando o AP velho for
   * desligado.
   */
  onConfiguredNetwork: boolean;
  active: boolean;
  /** Derivado do último heartbeat no backend, não persistido. */
  online: boolean;
  lastSeenAt: string | null;
  createdAt: string;
  /**
   * Broker MQTT específico desta cadeira; nulo usa o padrão global (o mesmo
   * que o firmware já traz embutido em config.h) — o caso comum, que a
   * maioria das cadeiras nunca precisa sair de.
   */
  mqttHost: string | null;
  mqttPort: number | null;
  mqttUsername: string | null;
  /** Quando o ESP32 confirmou ter gravado a configuração de MQTT. */
  mqttSyncedAt: string | null;
};

/** Espelha `ChairFilterDTO`. */
export type ChairFilter = {
  name?: string;
  active?: boolean;
  online?: boolean;
};

export type ListChairsParams = ChairFilter & PageParams;

/**
 * Espelha `SaveChairRequestDTO` — edição completa, exclusiva da equipe da
 * plataforma. Inclui `companyId`: reatribuir a empresa dona é edição, não só
 * cadastro — a cadeira pode mudar de cliente fisicamente. `ipAddress` e
 * `port` são opcionais: o heartbeat do ESP32 preenche assim que o dispositivo
 * se anuncia.
 */
export type SaveChairInput = {
  name: string;
  macAddress: string;
  companyId: number;
  ipAddress?: string;
  port?: number;
  /** Versão instalada. Opcional: nem toda cadeira passou pela atualização formal. */
  firmwareId?: number;
  /** Em branco deixa o ESP32 escolher o AP de melhor sinal. */
  wifiBssid?: string;
  /** Broker MQTT específico desta cadeira. Em branco usa o padrão global. */
  mqttHost?: string;
  mqttPort?: number;
  mqttUsername?: string;
  /** Em branco mantém a senha já gravada — não precisa redigitar a cada edição. */
  mqttPassword?: string;
};

/** Espelha `CreateChairRequestDTO`: mesmo formato do cadastro, exclusivo da equipe da plataforma. */
export type CreateChairInput = SaveChairInput;

/** Espelha `RenameChairRequestDTO`: o único campo que o RH edita. */
export type RenameChairInput = { name: string };

/** Empresa para o select da equipe da plataforma no cadastro. */
export type CompanyOption = { id: number; name: string };

export type ChairFieldErrors = Partial<
  Record<
    | "name"
    | "macAddress"
    | "ipAddress"
    | "port"
    | "firmwareId"
    | "wifiBssid"
    | "companyId"
    | "mqttHost"
    | "mqttPort"
    | "mqttUsername"
    | "mqttPassword",
    string
  >
>;

/** Estado dos formulários de cadastro e edição. */
export type ChairFormState = {
  status: "idle" | "success" | "error";
  message?: string;
  fieldErrors?: ChairFieldErrors;
};

export const CHAIR_INITIAL_STATE: ChairFormState = { status: "idle" };

/**
 * Versão no select do formulário de cadeira.
 *
 * <p>
 * Só o essencial do `FirmwareResponseDTO`: a tela de cadeiras não precisa das
 * notas nem dos binários, e mandar o registro inteiro ao cliente carregaria
 * dado do catálogo à toa.
 */
export type FirmwareOption = {
  id: number;
  version: string;
  productName: string;
};

/**
 * Espelha `ChairNetworkResultDTO`.
 *
 * <p>
 * Uma linha por equipamento porque o envio em lote continua mesmo quando uma
 * cadeira não responde: mostra quais foram e quais faltaram, em vez de um
 * "falhou" que não diz onde ir olhar.
 */
export type ChairNetworkResult = {
  chairId: number;
  chairName: string;
  delivered: boolean;
  /** Código do `Outcome` do backend, para distinguir os casos sem ler texto. */
  outcome: string;
  message: string;
};

/** Espelha `ChairMqttResultDTO`. */
export type ChairMqttResult = {
  chairId: number;
  chairName: string;
  delivered: boolean;
  outcome: string;
  message: string;
};
