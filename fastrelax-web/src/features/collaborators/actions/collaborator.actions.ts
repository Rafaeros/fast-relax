"use server";

import { revalidatePath } from "next/cache";
import { emptyPageSlice, toPageSlice, type PageSlice } from "@/lib/api/pagination.types";
import {
  createCollaborator,
  deleteCollaborator,
  listCollaborators,
  toggleCollaboratorActive,
  updateCollaborator,
} from "@/features/collaborators/services/collaborator.service";
import { replaceWeeklySchedule } from "@/features/collaborators/services/schedule.service";
import {
  mapApiFieldErrors,
  parseAllowedWindows,
  validateCollaboratorInput,
  validateCollaboratorUpdateInput,
} from "@/features/collaborators/schemas/collaborator.schema";
import type {
  Collaborator,
  CollaboratorFilter,
  CreateCollaboratorState,
  MutationResult,
} from "@/features/collaborators/types/collaborator.types";

/**
 * Página de colaboradores para a rolagem infinita.
 * Devolve sempre uma fatia — falha de API vira lista vazia sem `hasMore`, para
 * a tabela parar de pedir mais em vez de entrar em laço.
 */
export async function fetchCollaboratorsPage(
  page: number,
  filter: CollaboratorFilter = {},
): Promise<PageSlice<Collaborator>> {
  const result = await listCollaborators({ ...filter, page });

  if (!result.ok) {
    return emptyPageSlice<Collaborator>();
  }

  return toPageSlice(result.data);
}

/**
 * Cadastra o colaborador. Assinatura de `useActionState`.
 *
 * CPF duplicado é decidido pelo backend (`BusinessException` → 400 com
 * `status: "warning"`): a mensagem dele vem direto para o campo de CPF, sem
 * consulta prévia que abriria janela para corrida entre dois cadastros.
 */
export async function createCollaboratorAction(
  _previousState: CreateCollaboratorState,
  formData: FormData,
): Promise<CreateCollaboratorState> {
  const validation = validateCollaboratorInput({
    name: String(formData.get("name") ?? ""),
    cpf: String(formData.get("cpf") ?? ""),
    phoneNumber: String(formData.get("phoneNumber") ?? ""),
    departmentId: String(formData.get("departmentId") ?? ""),
  });

  if (!validation.valid) {
    return {
      status: "error",
      message: "Confira os campos destacados.",
      fieldErrors: validation.fieldErrors,
    };
  }

  const result = await createCollaborator(validation.data);

  if (!result.ok) {
    const fieldErrors = mapApiFieldErrors(result.errors);

    // Mensagem de negócio sem campo associado (CPF já cadastrado) marca o CPF.
    if (Object.keys(fieldErrors).length === 0 && /cpf/i.test(result.message)) {
      fieldErrors.cpf = result.message;
    }

    return { status: "error", message: result.message, fieldErrors };
  }

  // Horário permitido é recurso separado (`PUT /collaborators/{id}/schedules`).
  // O colaborador já existe neste ponto: se a semana falhar, o cadastro fica de
  // pé e o aviso diz o que resta fazer, em vez de sugerir refazer tudo.
  const windows = parseAllowedWindows(String(formData.get("allowedWindows") ?? ""));

  if (windows.length > 0) {
    const scheduleResult = await replaceWeeklySchedule(result.data.id, windows);

    if (!scheduleResult.ok) {
      revalidatePath("/painel/colaboradores");
      return {
        status: "error",
        message: `Colaborador cadastrado, mas o horário permitido não foi salvo: ${scheduleResult.message}`,
        fieldErrors: { allowedWindows: scheduleResult.message },
      };
    }
  }

  revalidatePath("/painel/colaboradores");
  return { status: "success", message: result.message };
}

/**
 * Atualiza o colaborador. O id chega em campo oculto do formulário.
 * CPF em branco mantém o atual; preenchido e já usado por outro cadastro, o
 * backend recusa e a mensagem volta marcada no campo.
 */
export async function updateCollaboratorAction(
  _previousState: CreateCollaboratorState,
  formData: FormData,
): Promise<CreateCollaboratorState> {
  const id = Number(formData.get("id"));

  if (!id || Number.isNaN(id)) {
    return { status: "error", message: "Colaborador não identificado." };
  }

  const validation = validateCollaboratorUpdateInput({
    name: String(formData.get("name") ?? ""),
    cpf: String(formData.get("cpf") ?? ""),
    phoneNumber: String(formData.get("phoneNumber") ?? ""),
    departmentId: String(formData.get("departmentId") ?? ""),
    active: String(formData.get("active") ?? "true"),
  });

  if (!validation.valid) {
    return {
      status: "error",
      message: "Confira os campos destacados.",
      fieldErrors: validation.fieldErrors,
    };
  }

  const result = await updateCollaborator(id, validation.data);

  if (!result.ok) {
    const fieldErrors = mapApiFieldErrors(result.errors);

    if (Object.keys(fieldErrors).length === 0 && /cpf/i.test(result.message)) {
      fieldErrors.cpf = result.message;
    }

    return { status: "error", message: result.message, fieldErrors };
  }

  // Horário permitido é recurso separado. Lista vazia não vira chamada: o
  // `@NotEmpty` do backend recusaria, então o horário atual permanece.
  const windows = parseAllowedWindows(String(formData.get("allowedWindows") ?? ""));

  if (windows.length > 0) {
    const scheduleResult = await replaceWeeklySchedule(id, windows);

    if (!scheduleResult.ok) {
      revalidatePath("/painel/colaboradores");
      return {
        status: "error",
        message: `Dados salvos, mas o horário permitido não foi atualizado: ${scheduleResult.message}`,
        fieldErrors: { allowedWindows: scheduleResult.message },
      };
    }
  }

  revalidatePath("/painel/colaboradores");
  return { status: "success", message: result.message };
}

export async function toggleCollaboratorActiveAction(id: number): Promise<MutationResult> {
  const result = await toggleCollaboratorActive(id);
  if (result.ok) revalidatePath("/painel/colaboradores");

  return { ok: result.ok, message: result.message };
}

export async function deleteCollaboratorAction(id: number): Promise<MutationResult> {
  const result = await deleteCollaborator(id);
  if (result.ok) revalidatePath("/painel/colaboradores");

  return { ok: result.ok, message: result.message };
}
