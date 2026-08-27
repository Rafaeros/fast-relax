import type { PageParams } from "@/lib/api/pagination.types";
import type { CollaboratorFieldErrors } from "@/features/collaborators/schemas/collaborator.schema";
import type { CredentialDelivery } from "@/features/authentication/types/auth.types";

/** Espelha `CollaboratorResponseDTO` do fastrelax-api. */
export type Collaborator = {
  id: number;
  departmentId: number | null;
  departmentName: string | null;
  name: string;
  /** Vem decriptado pela API, sem máscara (11 dígitos). */
  cpf: string;
  phoneNumber: string;
  /** Nulo quando não foi informado; sem ele não há recuperação de senha. */
  email: string | null;
  /** Verdadeiro enquanto o colaborador ainda não definiu a própria senha. */
  mustChangePassword: boolean;
  active: boolean;
  createdAt: string;
  deletedAt: string | null;
};

/**
 * Espelha `CreatedCollaboratorResponseDTO`.
 *
 * A senha temporária aparece só nesta resposta: o banco guarda apenas o hash,
 * então perder o valor significa ter de redefinir.
 */
export type CreatedCollaborator = {
  collaborator: Collaborator;
  credential: CredentialDelivery;
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
  /**
   * Opcional. Preenchido, a pessoa recebe convite e define a própria senha; em
   * branco, o sistema gera uma temporária para o RH repassar.
   */
  email?: string;
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
  /** Em branco remove o e-mail — e com ele a recuperação de senha. */
  email?: string;
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
  /**
   * Como o acesso foi entregue: convite por e-mail ou senha temporária. Quando é
   * senha, ela aparece uma única vez — o banco guarda apenas o hash.
   */
  credential?: CredentialDelivery;
};

export const CREATE_COLLABORATOR_INITIAL_STATE: CreateCollaboratorState = { status: "idle" };

/** Retorno das mutações simples (ativar/desativar, excluir). */
export type MutationResult = { ok: boolean; message: string };
