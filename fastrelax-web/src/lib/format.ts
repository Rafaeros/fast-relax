/** Formatações de exibição em pt-BR. Entrada crua vem da API sem máscara. */

/** 12345678901 → 123.456.789-01 */
export function formatCpf(cpf: string): string {
  const digits = cpf.replace(/\D/g, "");
  if (digits.length !== 11) return cpf;
  return digits.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, "$1.$2.$3-$4");
}

/** Mostra só os quatro últimos dígitos: 123.456.789-01 → •••.•••.789-01 */
export function maskCpf(cpf: string): string {
  const digits = cpf.replace(/\D/g, "");
  if (digits.length !== 11) return cpf;
  return `•••.•••.${digits.slice(6, 9)}-${digits.slice(9)}`;
}

/**
 * Máscara progressiva de CPF, para digitação: aplica pontuação conforme o
 * usuário escreve e descarta qualquer caractere que não seja dígito.
 */
export function maskCpfInput(value: string): string {
  const digits = value.replace(/\D/g, "").slice(0, 11);

  return digits
    .replace(/^(\d{3})(\d)/, "$1.$2")
    .replace(/^(\d{3})\.(\d{3})(\d)/, "$1.$2.$3")
    .replace(/\.(\d{3})(\d{1,2})$/, ".$1-$2");
}

/** Máscara progressiva de telefone: (11) 9 9000-1234 conforme digita. */
export function maskPhoneInput(value: string): string {
  const digits = value.replace(/\D/g, "").slice(0, 11);

  if (digits.length <= 2) return digits.replace(/^(\d{0,2})/, "($1");
  if (digits.length <= 6) return digits.replace(/^(\d{2})(\d{0,4})/, "($1) $2");
  if (digits.length <= 10) return digits.replace(/^(\d{2})(\d{4})(\d{0,4})/, "($1) $2-$3");
  return digits.replace(/^(\d{2})(\d{5})(\d{0,4})/, "($1) $2-$3");
}

const longDateFormatter = new Intl.DateTimeFormat("pt-BR", {
  day: "2-digit",
  month: "long",
  year: "numeric",
});

/** Data por extenso em pt-BR. Valor ausente ou inválido vira travessão. */
export function formatLongDate(value: string | null | undefined): string {
  if (!value) return "—";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "—" : longDateFormatter.format(date);
}

/** Remove tudo que não é dígito — o backend recebe CPF e telefone crus. */
export function onlyDigits(value: string): string {
  return value.replace(/\D/g, "");
}

/** 11990001234 → (11) 99000-1234 */
export function formatPhone(phone: string): string {
  const digits = phone.replace(/\D/g, "");
  if (digits.length === 11) {
    return digits.replace(/(\d{2})(\d{5})(\d{4})/, "($1) $2-$3");
  }
  if (digits.length === 10) {
    return digits.replace(/(\d{2})(\d{4})(\d{4})/, "($1) $2-$3");
  }
  return phone;
}
