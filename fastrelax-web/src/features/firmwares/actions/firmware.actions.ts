"use server";

import { revalidatePath } from "next/cache";
import { emptyPageSlice, toPageSlice, type PageSlice } from "@/lib/api/pagination.types";
import {
  createFirmware,
  deleteFirmwareFile,
  deleteFirmware,
  getFirmware,
  listFirmwares,
  updateFirmware,
  uploadFirmwareFile,
} from "@/features/firmwares/services/firmware.service";
import {
  mapFirmwareApiErrors,
  validateFirmwareInput,
} from "@/features/firmwares/schemas/firmware.schema";
import type { Firmware, FirmwareFormState } from "@/features/firmwares/types/firmware.types";
import type { MutationResult } from "@/features/collaborators/types/collaborator.types";

const ROUTE = "/painel/firmwares";

/**
 * Página de firmwares para a rolagem infinita.
 * Falha de API vira lista vazia sem `hasMore`, para a tabela parar de pedir
 * mais em vez de entrar em laço.
 */
export async function fetchFirmwaresPage(page: number): Promise<PageSlice<Firmware>> {
  const result = await listFirmwares({ page });
  return result.ok ? toPageSlice(result.data) : emptyPageSlice<Firmware>();
}

export async function createFirmwareAction(
  _previousState: FirmwareFormState,
  formData: FormData,
): Promise<FirmwareFormState> {
  const validation = readAndValidate(formData);

  if (!validation.valid) {
    return {
      status: "error",
      message: "Confira os campos destacados.",
      fieldErrors: validation.fieldErrors,
    };
  }

  const result = await createFirmware(validation.data);

  if (!result.ok) {
    return errorState(result.message, result.errors);
  }

  revalidatePath(ROUTE);
  // O id volta para o modal conseguir anexar o binário logo em seguida: o
  // upload é por id, e ele só passa a existir agora.
  return { status: "success", message: result.message, firmwareId: result.data.id };
}

/** Atualiza a versão. O id chega em campo oculto do formulário. */
export async function updateFirmwareAction(
  _previousState: FirmwareFormState,
  formData: FormData,
): Promise<FirmwareFormState> {
  const id = Number(formData.get("id"));

  if (!id || Number.isNaN(id)) {
    return { status: "error", message: "Firmware não identificado." };
  }

  const validation = readAndValidate(formData);

  if (!validation.valid) {
    return {
      status: "error",
      message: "Confira os campos destacados.",
      fieldErrors: validation.fieldErrors,
    };
  }

  const result = await updateFirmware(id, validation.data);

  if (!result.ok) {
    return errorState(result.message, result.errors);
  }

  revalidatePath(ROUTE);
  return { status: "success", message: result.message };
}

export async function deleteFirmwareAction(id: number): Promise<MutationResult> {
  const result = await deleteFirmware(id);
  if (result.ok) revalidatePath(ROUTE);

  return { ok: result.ok, message: result.message };
}

function readAndValidate(formData: FormData) {
  return validateFirmwareInput({
    productName: String(formData.get("productName") ?? ""),
    version: String(formData.get("version") ?? ""),
    releaseNotes: String(formData.get("releaseNotes") ?? ""),
    releaseDate: String(formData.get("releaseDate") ?? ""),
  });
}

/**
 * Anexa um binário à versão.
 *
 * <p>
 * O arquivo chega como `File` dentro do `FormData` da Server Action e segue
 * inteiro para a API. Nome, tamanho e SHA-256 são resolvidos lá — o hash é o
 * que o ESP32 confere antes de gravar, e calculá-lo aqui deixaria a conferência
 * dependente do que o cliente afirmou.
 */
export async function uploadFirmwareFileAction(
  firmwareId: number,
  formData: FormData,
): Promise<MutationResult> {
  const file = formData.get("file");

  if (!(file instanceof File) || file.size === 0) {
    return { ok: false, message: "Selecione um arquivo .bin ou .hex." };
  }

  const result = await uploadFirmwareFile(firmwareId, file);
  if (result.ok) revalidatePath(ROUTE);

  return { ok: result.ok, message: result.message };
}

export async function deleteFirmwareFileAction(
  firmwareId: number,
  fileId: number,
): Promise<MutationResult> {
  const result = await deleteFirmwareFile(firmwareId, fileId);
  if (result.ok) revalidatePath(ROUTE);

  return { ok: result.ok, message: result.message };
}

/**
 * Versão duplicada é decidida pelo backend (`BusinessException` → 400): sem
 * campo identificado na resposta, a mensagem cai na versão, que é o conflito
 * mais provável.
 */
function errorState(message: string, errors: string[]): FirmwareFormState {
  const fieldErrors = mapFirmwareApiErrors(errors);

  if (Object.keys(fieldErrors).length === 0) {
    fieldErrors.version = message;
  }

  return { status: "error", message, fieldErrors };
}

/**
 * Relê uma versão.
 *
 * <p>
 * O modal de detalhes usa isto depois de anexar ou remover um binário: a tabela
 * recarrega por conta, mas o registro que o modal está exibindo foi capturado
 * na abertura e ficaria com a lista de arquivos velha.
 */
export async function fetchFirmwareAction(id: number): Promise<Firmware | null> {
  const result = await getFirmware(id);
  return result.ok ? result.data : null;
}
