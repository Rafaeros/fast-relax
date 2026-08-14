import type {
  DepartmentFieldErrors,
  UpdateDepartmentInput,
} from "@/features/departments/types/department.types";

export type DepartmentValidation =
  | { valid: true; data: UpdateDepartmentInput }
  | { valid: false; fieldErrors: DepartmentFieldErrors };

/** Espelha as constraints de `CreateDepartmentDTO` / `DepartmentRequestDTO`. */
export function validateDepartmentInput(input: {
  name: string;
  active?: string;
}): DepartmentValidation {
  const name = input.name.trim();
  const fieldErrors: DepartmentFieldErrors = {};

  if (name.length < 2 || name.length > 100) {
    fieldErrors.name = "O nome deve ter entre 2 e 100 caracteres.";
  }

  if (Object.keys(fieldErrors).length > 0) {
    return { valid: false, fieldErrors };
  }

  // Sem o campo (cadastro), o departamento nasce ativo.
  return { valid: true, data: { name, active: input.active === undefined ? true : input.active === "true" } };
}

/** Converte os erros do backend (`"campo: mensagem"`) em erro por campo. */
export function mapDepartmentApiErrors(errors: string[]): DepartmentFieldErrors {
  const fieldErrors: DepartmentFieldErrors = {};

  for (const entry of errors) {
    const [field, ...rest] = entry.split(":");
    const message = rest.join(":").trim();
    if (message && field.trim() === "name") {
      fieldErrors.name = message;
    }
  }

  return fieldErrors;
}
