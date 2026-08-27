import { apiFetch } from "@/lib/api/http";
import { env } from "@/lib/env";
import type { ApiResult } from "@/lib/api/api.types";
import type { SpringPage } from "@/lib/api/pagination.types";
import { buildQuery } from "@/lib/api/query";
import { readAccessToken } from "@/features/authentication/services/session.service";
import type {
  Firmware,
  FirmwareFile,
  ListFirmwaresParams,
  SaveFirmwareInput,
} from "@/features/firmwares/types/firmware.types";

/**
 * Acesso à API de firmwares (`/firmwares`).
 *
 * <p>
 * Leitura é aberta a qualquer autenticado — a empresa precisa saber o que roda
 * nas cadeiras dela. Escrever é da equipe da plataforma: o catálogo é do
 * produto, não de um cliente.
 */

const RESOURCE = "/firmwares";

export const FIRMWARES_PAGE_SIZE = 50;

export async function listFirmwares(
  params: ListFirmwaresParams = {},
): Promise<ApiResult<SpringPage<Firmware>>> {
  const { page = 0, size = FIRMWARES_PAGE_SIZE, sort = "releaseDate,desc" } = params;

  return apiFetch<SpringPage<Firmware>>(`${RESOURCE}${buildQuery({ page, size, sort })}`, {
    token: await readAccessToken(),
  });
}

/**
 * Versões para preencher o select da cadeira.
 * Página grande de propósito: a lista alimenta um `<select>`, não uma tabela.
 */
export function listFirmwareOptions(): Promise<ApiResult<SpringPage<Firmware>>> {
  return listFirmwares({ page: 0, size: 200 });
}

export async function getFirmware(id: number): Promise<ApiResult<Firmware>> {
  return apiFetch<Firmware>(`${RESOURCE}/${id}`, { token: await readAccessToken() });
}

export async function createFirmware(input: SaveFirmwareInput): Promise<ApiResult<Firmware>> {
  return apiFetch<Firmware>(RESOURCE, {
    method: "POST",
    body: input,
    token: await readAccessToken(),
  });
}

export async function updateFirmware(
  id: number,
  input: SaveFirmwareInput,
): Promise<ApiResult<Firmware>> {
  return apiFetch<Firmware>(`${RESOURCE}/${id}`, {
    method: "PUT",
    body: input,
    token: await readAccessToken(),
  });
}

/**
 * Soft delete no backend. As cadeiras que apontam para esta versão continuam
 * apontando: é o registro do que está gravado em campo, e é o que o suporte
 * precisa saber.
 */
export async function deleteFirmware(id: number): Promise<ApiResult<null>> {
  return apiFetch<null>(`${RESOURCE}/${id}`, {
    method: "DELETE",
    token: await readAccessToken(),
  });
}

/**
 * Anexa um binário à versão.
 *
 * <p>
 * `FormData` sobe intacto pelo `apiFetch`, que detecta multipart e deixa o
 * browser montar o boundary. Tamanho e SHA-256 são calculados no servidor: o
 * hash é o que o ESP32 confere antes de gravar, e aceitar um valor vindo do
 * cliente tornaria a checagem decorativa.
 */
export async function uploadFirmwareFile(
  firmwareId: number,
  file: File,
): Promise<ApiResult<FirmwareFile>> {
  const body = new FormData();
  body.append("file", file);

  return apiFetch<FirmwareFile>(`${RESOURCE}/${firmwareId}/files`, {
    method: "POST",
    body,
    token: await readAccessToken(),
  });
}

export async function deleteFirmwareFile(
  firmwareId: number,
  fileId: number,
): Promise<ApiResult<null>> {
  return apiFetch<null>(`${RESOURCE}/${firmwareId}/files/${fileId}`, {
    method: "DELETE",
    token: await readAccessToken(),
  });
}

/**
 * Bytes do binário.
 *
 * <p>
 * Devolve `ArrayBuffer` cru, fora do envelope da API: quem consome é o download
 * do navegador e o gravador via Web Serial, e os dois querem o arquivo como
 * está. Por isso não passa pelo `apiFetch`, que desempacota JSON.
 */
export async function fetchFirmwareFileContent(
  firmwareId: number,
  fileId: number,
): Promise<{ ok: true; data: ArrayBuffer } | { ok: false; message: string }> {
  const token = await readAccessToken();

  const response = await fetch(
    `${env.apiUrl}${RESOURCE}/${firmwareId}/files/${fileId}/content`,
    {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      cache: "no-store",
    },
  );

  if (!response.ok) {
    return { ok: false, message: "Não foi possível baixar o arquivo do firmware." };
  }

  return { ok: true, data: await response.arrayBuffer() };
}
