import type { PageParams } from "@/lib/api/pagination.types";
import type { CollaboratorFieldErrors } from "@/features/collaborators/schemas/collaborator.schema";

/** Espelha `CollaboratorResponseDTO` do fastrelax-api. */
export type Collaborator = {
  id: number;
  departmentId: number | null;
  departmentName: string | null;
  name: string;
  /** Vem decriptado pela API, sem máscara (11 dígitos). */
  cpf: string;
  phoneNumber: string;
  active: boolean;
  createdAt: string;
  deletedAt: string | null;
};

/** Espelha `CollaboratorFilterDTO` — todos os campos são opcionais. */
export type CollaboratorFilter = {
  departmentId?: number;
  name?: string;
  cpf?: string;
  phoneNumber?: string;
  active?: boolean;
};

export type ListCollaboratorsParams = CollaboratorFilter & PageParams;

/** Espelha `CreateCollaboratorRequestDTO`. CPF sem pontuação (11 dígitos). */
export type CreateCollaboratorInput = {
  name: string;
  cpf: string;
  phoneNumber: string;
  departmentId?: number;
};

/**
 * Espelha `UpdateCollaboratorDTO`.
 * `active` é primitivo no backend: omitir o campo gravaria `false` e desativaria
 * o colaborador sem querer, então ele sempre viaja no corpo.
 * `cpf` vazio mantém o atual — só informe para corrigir cadastro errado.
 */
export type UpdateCollaboratorInput = {
  departmentId: number;
  name: string;
  phoneNumber: string;
  active: boolean;
  cpf?: string;
};

/**
 * Estado do formulário de cadastro.
 * Mora aqui, e não junto das actions: arquivo `"use server"` só pode exportar
 * funções async — constante ou tipo exportado de lá quebra o build.
 */
export type CreateCollaboratorState = {
  status: "idle" | "success" | "error";
  message?: string;
  fieldErrors?: CollaboratorFieldErrors;
};

export const CREATE_COLLABORATOR_INITIAL_STATE: CreateCollaboratorState = { status: "idle" };

/** Retorno das mutações simples (ativar/desativar, excluir). */
export type MutationResult = { ok: boolean; message: string };
