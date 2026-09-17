import type {
  ChairFieldErrors,
  RenameChairInput,
  SaveChairInput,
} from "@/features/chairs/types/chair.types";

export type ChairValidation =
  | { valid: true; data: SaveChairInput }
  | { valid: false; fieldErrors: ChairFieldErrors };

/** Cadastro e edição completa usam a mesma validação — os dois exigem empresa. */
export type CreateChairValidation = ChairValidation;

export type RenameChairValidation =
  | { valid: true; data: RenameChairInput }
  | { valid: false; fieldErrors: ChairFieldErrors };

/** Mesmo formato exigido pelo `@Pattern` de `SaveChairRequestDTO`. */
const MAC_PATTERN = /^([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}$/;

/**
 * IPv4 simples. Não cobre IPv6 de propósito: a rede é local e o ESP32 sempre
 * reporta IPv4.
 */
const IPV4_PATTERN =
  /^((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)\.){3}(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)$/;

/**
 * Espelha as constraints de `SaveChairRequestDTO`/`CreateChairRequestDTO` —
 * os dois exigem empresa: cadastro escolhe a dona, edição pode reatribuir.
 */
export function validateChairInput(input: {
  name: string;
  macAddress: string;
  companyId: string;
  ipAddress?: string;
  port?: string;
  firmwareId?: string;
  wifiBssid?: string;
  mqttHost?: string;
  mqttPort?: string;
  mqttUsername?: string;
  mqttPassword?: string;
}): ChairValidation {
  const name = input.name.trim();
  // O backend grava sempre em maiúsculas com dois-pontos; normalizar aqui evita
  // que a mesma cadeira pareça diferente conforme a digitação.
  const macAddress = input.macAddress.trim().toUpperCase().replace(/-/g, ":");
  const companyId = Number(input.companyId);
  const ipAddress = input.ipAddress?.trim() ?? "";
  const rawPort = input.port?.trim() ?? "";

  const fieldErrors: ChairFieldErrors = {};

  if (name.length < 2 || name.length > 100) {
    fieldErrors.name = "O nome deve ter entre 2 e 100 caracteres.";
  }

  if (!MAC_PATTERN.test(macAddress)) {
    fieldErrors.macAddress = "Informe no formato AA:BB:CC:DD:EE:FF.";
  }

  if (!Number.isInteger(companyId) || companyId <= 0) {
    fieldErrors.companyId = "Selecione a empresa dona do equipamento.";
  }

  if (ipAddress && !IPV4_PATTERN.test(ipAddress)) {
    fieldErrors.ipAddress = "Informe um IP válido (ex.: 192.168.1.50).";
  }

  let port: number | undefined;
  if (rawPort) {
    port = Number(rawPort);
    if (!Number.isInteger(port) || port < 1 || port > 65535) {
      fieldErrors.port = "A porta deve estar entre 1 e 65535.";
    }
  }

  if (Object.keys(fieldErrors).length > 0) {
    return { valid: false, fieldErrors };
  }

  // Campo em branco significa "sem versão registrada", não zero.
  const firmwareId = Number(input.firmwareId);

  // Normalizado como o MAC, e pelo mesmo motivo: o backend grava em maiúsculas
  // com dois-pontos, e sem isso o mesmo AP pareceria dois valores diferentes.
  const wifiBssid = (input.wifiBssid ?? "").trim().toUpperCase().replace(/-/g, ":");
  if (wifiBssid && !MAC_PATTERN.test(wifiBssid)) {
    fieldErrors.wifiBssid = "Informe no formato AA:BB:CC:DD:EE:FF.";
  }

  const mqttHost = (input.mqttHost ?? "").trim();
  const rawMqttPort = input.mqttPort?.trim() ?? "";
  let mqttPort: number | undefined;
  if (rawMqttPort) {
    mqttPort = Number(rawMqttPort);
    if (!Number.isInteger(mqttPort) || mqttPort < 1 || mqttPort > 65535) {
      fieldErrors.mqttPort = "A porta deve estar entre 1 e 65535.";
    }
  }
  const mqttUsername = (input.mqttUsername ?? "").trim();
  const mqttPassword = input.mqttPassword ?? "";

  if (Object.keys(fieldErrors).length > 0) {
    return { valid: false, fieldErrors };
  }

  return {
    valid: true,
    data: {
      name,
      macAddress,
      companyId,
      ipAddress: ipAddress || undefined,
      port,
      firmwareId: Number.isInteger(firmwareId) && firmwareId > 0 ? firmwareId : undefined,
      // String vazia viaja de propósito: é assim que se apaga a fixação de AP.
      wifiBssid,
      // Idem: string vazia em mqttHost é o que apaga o override no backend.
      mqttHost,
      mqttPort,
      mqttUsername,
      // Vazia mantém a senha já gravada — o backend só sobrescreve quando vem preenchida.
      mqttPassword: mqttPassword || undefined,
    },
  };
}

/** Cadastro, exclusivo da equipe da plataforma: mesma validação de `validateChairInput`. */
export const validateCreateChairInput = validateChairInput;

/** Edição do RH: só o nome, mesma regra de `validateChairInput`. */
export function validateRenameChairInput(input: { name: string }): RenameChairValidation {
  const name = input.name.trim();

  if (name.length < 2 || name.length > 100) {
    return { valid: false, fieldErrors: { name: "O nome deve ter entre 2 e 100 caracteres." } };
  }

  return { valid: true, data: { name } };
}

/** Converte os erros do backend (`"campo: mensagem"`) em erro por campo. */
export function mapChairApiErrors(errors: string[]): ChairFieldErrors {
  const fieldErrors: ChairFieldErrors = {};
  const known = new Set([
    "name",
    "macAddress",
    "ipAddress",
    "port",
    "firmwareId",
    "wifiBssid",
    "companyId",
    "mqttHost",
    "mqttPort",
    "mqttUsername",
    "mqttPassword",
  ]);

  for (const entry of errors) {
    const [field, ...rest] = entry.split(":");
    const key = field.trim();
    const message = rest.join(":").trim();

    if (message && known.has(key)) {
      fieldErrors[key as keyof ChairFieldErrors] = message;
    }
  }

  return fieldErrors;
}
