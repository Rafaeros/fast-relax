"use server";

import { revalidatePath } from "next/cache";
import { emptyPageSlice, toPageSlice, type PageSlice } from "@/lib/api/pagination.types";
import {
  createChair,
  deleteChair,
  listChairs,
  toggleChairActive,
  updateChair,
} from "@/features/chairs/services/chair.service";
import { mapChairApiErrors, validateChairInput } from "@/features/chairs/schemas/chair.schema";
import type { Chair, ChairFilter, ChairFormState } from "@/features/chairs/types/chair.types";
import type { MutationResult } from "@/features/collaborators/types/collaborator.types";

const ROUTE = "/painel/cadeiras";

/**
 * Página de cadeiras para a rolagem infinita.
 * Falha de API vira lista vazia sem `hasMore`, para a tabela parar de pedir
 * mais em vez de entrar em laço.
 */
export async function fetchChairsPage(
  page: number,
  filter: ChairFilter = {},
): Promise<PageSlice<Chair>> {
  const result = await listChairs({ ...filter, page });

  if (!result.ok) {
    return emptyPageSlice<Chair>();
  }

  return toPageSlice(result.data);
}

/** Lê os campos comuns de cadastro e edição do formulário. */
function readFormFields(formData: FormData) {
  return {
    name: String(formData.get("name") ?? ""),
    macAddress: String(formData.get("macAddress") ?? ""),
    ipAddress: String(formData.get("ipAddress") ?? ""),
    port: String(formData.get("port") ?? ""),
  };
}

/** Erro sem campo identificado cai no MAC, que é a causa mais provável. */
function toErrorState(message: string, errors: string[]): ChairFormState {
  const fieldErrors = mapChairApiErrors(errors);

  if (Object.keys(fieldErrors).length === 0) {
    fieldErrors.macAddress = message;
  }

  return { status: "error", message, fieldErrors };
}

/**
 * Cadastra a cadeira.
 * MAC repetido é decidido pelo backend (`BusinessException` → 400): a mensagem
 * dele volta marcada no campo, sem consulta prévia que abriria janela para
 * corrida entre dois cadastros.
 */
export async function createChairAction(
  _previousState: ChairFormState,
  formData: FormData,
): Promise<ChairFormState> {
  const validation = validateChairInput(readFormFields(formData));

  if (!validation.valid) {
    return {
      status: "error",
      message: "Confira os campos destacados.",
      fieldErrors: validation.fieldErrors,
    };
  }

  const result = await createChair(validation.data);

  if (!result.ok) {
    return toErrorState(result.message, result.errors);
  }

  revalidatePath(ROUTE);
  return { status: "success", message: result.message };
}

/** Atualiza os dados da cadeira. O id chega em campo oculto do formulário. */
export async function updateChairAction(
  _previousState: ChairFormState,
  formData: FormData,
): Promise<ChairFormState> {
  const id = Number(formData.get("id"));

  if (!id || Number.isNaN(id)) {
    return { status: "error", message: "Cadeira não identificada." };
  }

  const validation = validateChairInput(readFormFields(formData));

  if (!validation.valid) {
    return {
      status: "error",
      message: "Confira os campos destacados.",
      fieldErrors: validation.fieldErrors,
    };
  }

  const result = await updateChair(id, validation.data);

  if (!result.ok) {
    return toErrorState(result.message, result.errors);
  }

  revalidatePath(ROUTE);
  return { status: "success", message: result.message };
}

/**
 * Alterna a situação. Desativar tira a cadeira do rodízio de atendimento sem
 * apagar o histórico de sessões que ela atendeu.
 */
export async function toggleChairActiveAction(id: number): Promise<MutationResult> {
  const result = await toggleChairActive(id);
  if (result.ok) revalidatePath(ROUTE);

  return { ok: result.ok, message: result.message };
}

export async function deleteChairAction(id: number): Promise<MutationResult> {
  const result = await deleteChair(id);
  if (result.ok) revalidatePath(ROUTE);

  return { ok: result.ok, message: result.message };
}
