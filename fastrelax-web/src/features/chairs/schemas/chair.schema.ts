import type { ChairFieldErrors, SaveChairInput } from "@/features/chairs/types/chair.types";

export type ChairValidation =
  | { valid: true; data: SaveChairInput }
  | { valid: false; fieldErrors: ChairFieldErrors };

/** Mesmo formato exigido pelo `@Pattern` de `SaveChairRequestDTO`. */
const MAC_PATTERN = /^([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}$/;

/**
 * IPv4 simples. Não cobre IPv6 de propósito: a rede é local e o ESP32 sempre
 * reporta IPv4.
 */
const IPV4_PATTERN =
  /^((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)\.){3}(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)$/;

/** Espelha as constraints de `SaveChairRequestDTO`. */
export function validateChairInput(input: {
  name: string;
  macAddress: string;
  ipAddress?: string;
  port?: string;
  firmwareId?: string;
  wifiBssid?: string;
}): ChairValidation {
  const name = input.name.trim();
  // O backend grava sempre em maiúsculas com dois-pontos; normalizar aqui evita
  // que a mesma cadeira pareça diferente conforme a digitação.
  const macAddress = input.macAddress.trim().toUpperCase().replace(/-/g, ":");
  const ipAddress = input.ipAddress?.trim() ?? "";
  const rawPort = input.port?.trim() ?? "";

  const fieldErrors: ChairFieldErrors = {};

  if (name.length < 2 || name.length > 100) {
    fieldErrors.name = "O nome deve ter entre 2 e 100 caracteres.";
  }

  if (!MAC_PATTERN.test(macAddress)) {
    fieldErrors.macAddress = "Informe no formato AA:BB:CC:DD:EE:FF.";
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

  return {
    valid: true,
    data: {
      name,
      macAddress,
      ipAddress: ipAddress || undefined,
      port,
      firmwareId: Number.isInteger(firmwareId) && firmwareId > 0 ? firmwareId : undefined,
      // String vazia viaja de propósito: é assim que se apaga a fixação de AP.
      wifiBssid,
    },
  };
}

/** Converte os erros do backend (`"campo: mensagem"`) em erro por campo. */
export function mapChairApiErrors(errors: string[]): ChairFieldErrors {
  const fieldErrors: ChairFieldErrors = {};
  const known = new Set(["name", "macAddress", "ipAddress", "port", "firmwareId", "wifiBssid"]);

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
