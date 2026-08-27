import { onlyDigits } from "@/lib/format";
import type {
  CompanyFieldErrors,
  SaveCompanyInput,
} from "@/features/companies/types/company.types";

export type CompanyValidation =
  | { valid: true; data: SaveCompanyInput }
  | { valid: false; fieldErrors: CompanyFieldErrors };

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

/** Espelha as constraints de `SaveCompanyRequestDTO`. */
export function validateCompanyInput(input: {
  cnpj: string;
  name: string;
  email: string;
  phone: string;
  cityId: string;
  cep: string;
  street: string;
  number: string;
  complement: string;
  wifiSsid: string;
  wifiPassword: string;
}): CompanyValidation {
  const cnpj = onlyDigits(input.cnpj);
  const name = input.name.trim();
  const email = input.email.trim().toLowerCase();
  const phone = onlyDigits(input.phone);
  const cep = onlyDigits(input.cep);
  const street = input.street.trim();
  const numberValue = input.number.trim();
  const complement = input.complement.trim();
  const cityId = Number(input.cityId);
  const wifiSsid = input.wifiSsid.trim();
  // A senha não é aparada: espaço é caractere válido em senha de Wi-Fi, e
  // aparar aqui gravaria no ESP32 algo diferente do que o AP espera.
  const wifiPassword = input.wifiPassword;

  const fieldErrors: CompanyFieldErrors = {};

  // Os 14 dígitos e os verificadores são conferidos aqui pelo mesmo motivo do
  // CPF: um CNPJ com dígito trocado cria uma empresa cujo login nunca funciona,
  // e corrigir depois exige mexer no cadastro inteiro.
  if (cnpj.length !== 14) {
    fieldErrors.cnpj = "Informe os 14 dígitos do CNPJ.";
  } else if (!hasValidCnpjCheckDigits(cnpj)) {
    fieldErrors.cnpj = "CNPJ inválido.";
  }

  if (name.length < 2 || name.length > 255) {
    fieldErrors.name = "O nome deve ter entre 2 e 255 caracteres.";
  }

  if (!email) {
    fieldErrors.email = "Informe o e-mail.";
  } else if (!EMAIL_PATTERN.test(email)) {
    fieldErrors.email = "E-mail inválido.";
  }

  if (phone.length < 10 || phone.length > 13) {
    fieldErrors.phone = "Informe um telefone válido com DDD.";
  }

  if (!Number.isInteger(cityId) || cityId <= 0) {
    fieldErrors.cityId = "Selecione a cidade.";
  }

  if (cep.length !== 8) {
    fieldErrors.cep = "Informe os 8 dígitos do CEP.";
  }

  if (!street) {
    fieldErrors.street = "Informe o logradouro.";
  }

  if (!numberValue) {
    fieldErrors.number = "Informe o número.";
  }

  if (wifiSsid.length > 64) {
    fieldErrors.wifiSsid = "O SSID deve ter no máximo 64 caracteres.";
  }

  if (wifiPassword.length > 128) {
    fieldErrors.wifiSsid = "A senha deve ter no máximo 128 caracteres.";
  }

  // Senha sem rede não tem onde ser usada, e guardá-la assim deixaria um
  // segredo do cliente no banco sem nada que o consuma.
  if (!wifiSsid && wifiPassword) {
    fieldErrors.wifiSsid = "Informe o SSID da rede antes da senha.";
  }

  if (Object.keys(fieldErrors).length > 0) {
    return { valid: false, fieldErrors };
  }

  return {
    valid: true,
    data: {
      cnpj,
      name,
      email,
      phone,
      address: {
        cityId,
        cep,
        street,
        number: numberValue,
        complement: complement || undefined,
      },
      wifiSsid,
      // Senha em branco não é "apagar": o backend lê ausência como "manter a
      // atual". Quem quiser tirar a rede limpa o SSID, e a senha cai junto.
      wifiPassword: wifiPassword || undefined,
    },
  };
}

/**
 * Dígitos verificadores do CNPJ (módulo 11, pesos 2..9 cíclicos).
 * Mesmo cálculo do `CnpjUtils` do backend.
 */
export function hasValidCnpjCheckDigits(digits: string): boolean {
  if (digits.length !== 14) return false;
  // Sequências repetidas satisfazem o cálculo por acidente.
  if (new Set(digits).size === 1) return false;

  return (
    checkDigit(digits, 12) === Number(digits[12]) && checkDigit(digits, 13) === Number(digits[13])
  );
}

function checkDigit(digits: string, position: number): number {
  let sum = 0;
  let weight = 2;

  for (let index = position - 1; index >= 0; index--) {
    sum += Number(digits[index]) * weight;
    weight = weight === 9 ? 2 : weight + 1;
  }

  const remainder = sum % 11;
  return remainder < 2 ? 0 : 11 - remainder;
}

/** Converte os erros do backend (`"campo: mensagem"`) em erro por campo. */
export function mapCompanyApiErrors(errors: string[]): CompanyFieldErrors {
  const fieldErrors: CompanyFieldErrors = {};
  const known: (keyof CompanyFieldErrors)[] = [
    "cnpj",
    "name",
    "email",
    "phone",
    "cityId",
    "cep",
    "street",
    "number",
    "wifiSsid",
  ];

  for (const entry of errors) {
    const [rawField, ...rest] = entry.split(":");
    const message = rest.join(":").trim();
    if (!message) continue;

    // O backend prefixa os campos do endereço ("address.cep"); a tela conhece
    // só o nome curto.
    const field = rawField.trim().replace(/^address\./, "") as keyof CompanyFieldErrors;
    if (known.includes(field)) {
      fieldErrors[field] = message;
    }
  }

  return fieldErrors;
}
