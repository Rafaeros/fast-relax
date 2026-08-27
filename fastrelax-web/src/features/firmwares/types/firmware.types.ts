import type { PageParams } from "@/lib/api/pagination.types";

/** Espelha `FirmwareFileResponseDTO` do fastrelax-api. */
export type FirmwareFile = {
  id: number;
  fileName: string;
  fileSize: number;
  /** SHA-256 em hexadecimal — é o que o ESP32 confere antes de gravar na flash. */
  fileHash: string;
  contentType: string | null;
  /**
   * Se dá para gravar direto no ESP32.
   *
   * Decidido pelo backend: o esptool trabalha com imagem binária, e Intel HEX
   * (`.hex`) é formato de AVR. Um `.hex` pode ser baixado, mas não gravado — e
   * é melhor a interface não oferecer o botão do que falhar no meio.
   */
  flashable: boolean;
};

/** Espelha `FirmwareResponseDTO`. */
export type Firmware = {
  id: number;
  productName: string;
  version: string;
  releaseNotes: string | null;
  releaseDate: string;
  files: FirmwareFile[];
  createdAt: string;
};

/**
 * Filtro da listagem.
 * `GET /firmwares` aceita só paginação: a busca é aplicada no cliente, sobre as
 * linhas já carregadas.
 */
export type FirmwareFilter = {
  search?: string;
};

export type ListFirmwaresParams = PageParams;

/**
 * Espelha `SaveFirmwareRequestDTO` — só metadados.
 *
 * Os binários entram e saem por rotas próprias: mandá-los junto faria cada
 * edição de nota de versão substituir a lista inteira de arquivos.
 */
export type SaveFirmwareInput = {
  productName: string;
  version: string;
  releaseNotes?: string;
  releaseDate: string;
};

export type FirmwareFieldErrors = Partial<
  Record<"productName" | "version" | "releaseDate", string>
>;

/** Estado dos formulários de cadastro e edição. */
export type FirmwareFormState = {
  status: "idle" | "success" | "error";
  message?: string;
  fieldErrors?: FirmwareFieldErrors;
  /**
   * Id da versão recém-publicada.
   *
   * <p>
   * O upload do binário precisa dele, e ele só existe depois do insert — é o
   * que permite anexar o arquivo no mesmo gesto do cadastro, sem obrigar a
   * abrir os detalhes em seguida.
   */
  firmwareId?: number;
};

export const FIRMWARE_INITIAL_STATE: FirmwareFormState = { status: "idle" };
