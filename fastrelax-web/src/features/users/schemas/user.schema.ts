import type {
  CreateUserInput,
  UpdateUserInput,
  UserFieldErrors,
  UserRole,
} from "@/features/users/types/user.types";

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;
const ROLES: UserRole[] = ["SYSADMIN", "COMPANY_ADMIN", "COMPANY_RH"];

export type CreateUserValidation =
  | { valid: true; data: CreateUserInput }
  | { valid: false; fieldErrors: UserFieldErrors };

/** Espelha as constraints de `CreateUserRequestDTO`. */
export function validateCreateUserInput(input: {
  name: string;
  email: string;
  role: string;
  companyId?: string;
}): CreateUserValidation {
  const name = input.name.trim();
  const email = input.email.trim().toLowerCase();
  const fieldErrors: UserFieldErrors = {};

  if (name.length < 2 || name.length > 120) {
    fieldErrors.name = "O nome deve ter entre 2 e 120 caracteres.";
  }

  if (!email) {
    fieldErrors.email = "Informe o e-mail.";
  } else if (!EMAIL_PATTERN.test(email)) {
    fieldErrors.email = "E-mail inválido.";
  } else if (email.length > 180) {
    fieldErrors.email = "O e-mail deve ter no máximo 180 caracteres.";
  }

  const role = ROLES.find((entry) => entry === input.role);
  if (!role) {
    fieldErrors.role = "Selecione o perfil.";
  }

  if (Object.keys(fieldErrors).length > 0 || !role) {
    return { valid: false, fieldErrors };
  }

  // Só o SYSADMIN informa a empresa; para quem opera dentro de uma, o backend
  // ignora o campo e usa a do contexto.
  const companyId = Number(input.companyId);
  return {
    valid: true,
    data: {
      name,
      email,
      role,
      companyId: Number.isInteger(companyId) && companyId > 0 ? companyId : undefined,
    },
  };
}

export type UpdateUserValidation =
  | { valid: true; data: UpdateUserInput }
  | { valid: false; fieldErrors: UserFieldErrors };

/**
 * Espelha `UpdateUserRequestDTO`: nome entre 2 e 100 (limite menor que o do
 * cadastro, é assim no backend) e email opcional — em branco mantém o atual.
 */
export function validateUpdateUserInput(input: {
  name: string;
  email: string;
}): UpdateUserValidation {
  const name = input.name.trim();
  const email = input.email.trim().toLowerCase();
  const fieldErrors: UserFieldErrors = {};

  if (name.length < 2 || name.length > 100) {
    fieldErrors.name = "O nome deve ter entre 2 e 100 caracteres.";
  }

  if (email && !EMAIL_PATTERN.test(email)) {
    fieldErrors.email = "E-mail inválido.";
  }

  if (Object.keys(fieldErrors).length > 0) {
    return { valid: false, fieldErrors };
  }

  return { valid: true, data: { name, email: email || undefined } };
}

/** Converte os erros do backend (`"campo: mensagem"`) em erro por campo. */
export function mapUserApiErrors(errors: string[]): UserFieldErrors {
  const fieldErrors: UserFieldErrors = {};

  for (const entry of errors) {
    const [field, ...rest] = entry.split(":");
    const message = rest.join(":").trim();
    if (!message) continue;

    switch (field.trim()) {
      case "name":
        fieldErrors.name = message;
        break;
      case "email":
        fieldErrors.email = message;
        break;
      case "role":
        fieldErrors.role = message;
        break;
    }
  }

  return fieldErrors;
}
