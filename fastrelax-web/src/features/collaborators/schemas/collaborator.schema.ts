import { onlyDigits } from "@/lib/format";
import type {
  CreateCollaboratorInput,
  UpdateCollaboratorInput,
} from "@/features/collaborators/types/collaborator.types";
import { WORK_DAYS, type AllowedWindow } from "@/features/collaborators/types/schedule.types";

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

export type CollaboratorFieldErrors = Partial<
  Record<keyof CreateCollaboratorInput | "allowedWindows", string>
>;

export type CollaboratorValidation =
  | { valid: true; data: CreateCollaboratorInput }
  | { valid: false; fieldErrors: CollaboratorFieldErrors };

/**
 * Dígitos verificadores do CPF — mesma checagem do `@Cpf` no backend.
 * Evita ida ao servidor para um CPF que já sabemos ser inválido.
 */
export function isValidCpf(rawCpf: string): boolean {
  const cpf = onlyDigits(rawCpf);
  if (cpf.length !== 11) return false;
  // Sequências repetidas passam no cálculo, mas não são CPFs válidos.
  if (/^(\d)\1{10}$/.test(cpf)) return false;

  const checkDigit = (length: number): number => {
    let sum = 0;
    for (let index = 0; index < length; index += 1) {
      sum += Number(cpf[index]) * (length + 1 - index);
    }
    const remainder = (sum * 10) % 11;
    return remainder === 10 ? 0 : remainder;
  };

  return checkDigit(9) === Number(cpf[9]) && checkDigit(10) === Number(cpf[10]);
}

/** Espelha as constraints de `CreateCollaboratorRequestDTO`. */
export function validateCollaboratorInput(input: {
  name: string;
  cpf: string;
  phoneNumber: string;
  email: string;
  departmentId: string;
}): CollaboratorValidation {
  const name = input.name.trim();
  const cpf = onlyDigits(input.cpf);
  const phoneNumber = onlyDigits(input.phoneNumber);
  const email = input.email.trim().toLowerCase();
  const departmentId = Number(input.departmentId);
  const fieldErrors: CollaboratorFieldErrors = {};

  // Campo opcional: só é validado quando preenchido. Vazio significa "sem
  // e-mail", e a pessoa recebe senha temporária em vez de convite.
  if (email && !EMAIL_PATTERN.test(email)) {
    fieldErrors.email = "E-mail inválido.";
  }

  if (name.length < 2 || name.length > 120) {
    fieldErrors.name = "O nome deve ter entre 2 e 120 caracteres.";
  }

  if (!cpf) {
    fieldErrors.cpf = "Informe o CPF.";
  } else if (cpf.length !== 11) {
    fieldErrors.cpf = "O CPF deve ter 11 dígitos.";
  } else if (!isValidCpf(cpf)) {
    fieldErrors.cpf = "CPF inválido.";
  }

  if (!phoneNumber) {
    fieldErrors.phoneNumber = "Informe o telefone.";
  } else if (phoneNumber.length < 10) {
    fieldErrors.phoneNumber = "Telefone incompleto.";
  }

  if (!departmentId || Number.isNaN(departmentId)) {
    fieldErrors.departmentId = "Selecione o departamento.";
  }

  if (Object.keys(fieldErrors).length > 0) {
    return { valid: false, fieldErrors };
  }

  return { valid: true, data: { name, cpf, phoneNumber, email: email || undefined, departmentId } };
}

export type UpdateValidation =
  | { valid: true; data: UpdateCollaboratorInput }
  | { valid: false; fieldErrors: CollaboratorFieldErrors };

/**
 * Espelha `UpdateCollaboratorDTO`: as mesmas regras da criação, exceto o CPF —
 * em branco significa "manter o atual", e só é validado quando preenchido.
 */
export function validateCollaboratorUpdateInput(input: {
  name: string;
  cpf: string;
  phoneNumber: string;
  email: string;
  departmentId: string;
  active: string;
}): UpdateValidation {
  const name = input.name.trim();
  const cpf = onlyDigits(input.cpf);
  const phoneNumber = onlyDigits(input.phoneNumber);
  const email = input.email.trim().toLowerCase();
  const departmentId = Number(input.departmentId);
  const fieldErrors: CollaboratorFieldErrors = {};

  if (email && !EMAIL_PATTERN.test(email)) {
    fieldErrors.email = "E-mail inválido.";
  }

  if (name.length < 2 || name.length > 120) {
    fieldErrors.name = "O nome deve ter entre 2 e 120 caracteres.";
  }

  if (cpf && cpf.length !== 11) {
    fieldErrors.cpf = "O CPF deve ter 11 dígitos.";
  } else if (cpf && !isValidCpf(cpf)) {
    fieldErrors.cpf = "CPF inválido.";
  }

  if (!phoneNumber) {
    fieldErrors.phoneNumber = "Informe o telefone.";
  } else if (phoneNumber.length < 10) {
    fieldErrors.phoneNumber = "Telefone incompleto.";
  }

  if (!departmentId || Number.isNaN(departmentId)) {
    fieldErrors.departmentId = "Selecione o departamento.";
  }

  if (Object.keys(fieldErrors).length > 0) {
    return { valid: false, fieldErrors };
  }

  return {
    valid: true,
    // String vazia viaja de propósito: é assim que a edição remove o e-mail.
    data: { name, cpf, phoneNumber, email, departmentId, active: input.active === "true" },
  };
}

/**
 * Lê a lista de horários permitidos que o widget serializa no formulário.
 * Entrada corrompida vira lista vazia: o cadastro segue, e o RH configura a
 * semana depois — melhor que perder o registro inteiro.
 */
export function parseAllowedWindows(raw: string): AllowedWindow[] {
  if (!raw) return [];

  try {
    const parsed: unknown = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];

    return parsed.filter((entry): entry is AllowedWindow => {
      if (typeof entry !== "object" || entry === null) return false;
      const window = entry as Partial<AllowedWindow>;

      return (
        typeof window.dayOfWeek === "string" &&
        WORK_DAYS.includes(window.dayOfWeek as AllowedWindow["dayOfWeek"]) &&
        typeof window.allowedStartTime === "string" &&
        typeof window.allowedEndTime === "string" &&
        window.allowedEndTime > window.allowedStartTime
      );
    });
  } catch {
    return [];
  }
}

/**
 * Converte os erros de validação do backend (`"campo: mensagem"`) em erros por
 * campo, para destacar o input certo em vez de só mostrar o texto solto.
 */
export function mapApiFieldErrors(errors: string[]): CollaboratorFieldErrors {
  const fieldErrors: CollaboratorFieldErrors = {};

  for (const entry of errors) {
    const [field, ...rest] = entry.split(":");
    const message = rest.join(":").trim();
    if (!message) continue;

    switch (field.trim()) {
      case "name":
        fieldErrors.name = message;
        break;
      case "cpf":
        fieldErrors.cpf = message;
        break;
      case "phoneNumber":
        fieldErrors.phoneNumber = message;
        break;
      case "departmentId":
        fieldErrors.departmentId = message;
        break;
    }
  }

  return fieldErrors;
}
