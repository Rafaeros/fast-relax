import type { PageParams } from "@/lib/api/pagination.types";

/** Espelha `DepartmentResponseDTO` do fastrelax-api. */
export type Department = {
  id: number;
  name: string;
  active: boolean;
  createdAt: string;
  deletedAt: string | null;
};

/** Espelha `DepartmentFilterDTO`. */
export type DepartmentFilter = {
  name?: string;
  active?: boolean;
};

export type ListDepartmentsParams = DepartmentFilter & PageParams;

/** Espelha `CreateDepartmentDTO`. */
export type CreateDepartmentInput = {
  name: string;
};

/**
 * Espelha `DepartmentRequestDTO`.
 * `active` é primitivo no backend: omitir gravaria `false` e desativaria o
 * departamento sem querer, então sempre viaja no corpo.
 */
export type UpdateDepartmentInput = {
  name: string;
  active: boolean;
};

export type DepartmentFieldErrors = Partial<Record<"name", string>>;

/** Estado dos formulários de cadastro e edição. */
export type DepartmentFormState = {
  status: "idle" | "success" | "error";
  message?: string;
  fieldErrors?: DepartmentFieldErrors;
};

export const DEPARTMENT_INITIAL_STATE: DepartmentFormState = { status: "idle" };
