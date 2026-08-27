import type {
  FirmwareFieldErrors,
  SaveFirmwareInput,
} from "@/features/firmwares/types/firmware.types";

export type FirmwareValidation =
  | { valid: true; data: SaveFirmwareInput }
  | { valid: false; fieldErrors: FirmwareFieldErrors };

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

/**
 * Espelha as constraints de `SaveFirmwareRequestDTO`.
 *
 * <p>
 * Só metadados: os binários são anexados por rota própria, e nome, tamanho e
 * hash passaram a ser calculados no servidor a partir do arquivo enviado — não
 * há mais o que validar aqui sobre eles.
 */
export function validateFirmwareInput(input: {
  productName: string;
  version: string;
  releaseNotes: string;
  releaseDate: string;
}): FirmwareValidation {
  const productName = input.productName.trim();
  const version = input.version.trim();
  const releaseNotes = input.releaseNotes.trim();
  const releaseDate = input.releaseDate.trim();

  const fieldErrors: FirmwareFieldErrors = {};

  if (!productName || productName.length > 100) {
    fieldErrors.productName = "Informe o produto (até 100 caracteres).";
  }

  if (!version || version.length > 50) {
    fieldErrors.version = "Informe a versão (até 50 caracteres).";
  }

  if (!ISO_DATE.test(releaseDate)) {
    fieldErrors.releaseDate = "Informe a data de publicação.";
  }

  if (Object.keys(fieldErrors).length > 0) {
    return { valid: false, fieldErrors };
  }

  return {
    valid: true,
    data: {
      productName,
      version,
      releaseNotes: releaseNotes || undefined,
      releaseDate,
    },
  };
}

/** Converte os erros do backend (`"campo: mensagem"`) em erro por campo. */
export function mapFirmwareApiErrors(errors: string[]): FirmwareFieldErrors {
  const fieldErrors: FirmwareFieldErrors = {};

  for (const entry of errors) {
    const [rawField, ...rest] = entry.split(":");
    const message = rest.join(":").trim();
    if (!message) continue;

    const field = rawField.trim();
    if (field === "productName" || field === "version" || field === "releaseDate") {
      fieldErrors[field] = message;
    }
  }

  return fieldErrors;
}
