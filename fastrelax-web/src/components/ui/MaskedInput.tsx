"use client";

import { useState, type ChangeEvent } from "react";
import { Input, type InputProps } from "@/components/ui/Input";
import { maskCepInput, maskCnpjInput, maskCpfInput, maskPhoneInput } from "@/lib/format";

export type MaskName = "cpf" | "cnpj" | "cep" | "phone";

const MASKS: Record<MaskName, (value: string) => string> = {
  cpf: maskCpfInput,
  cnpj: maskCnpjInput,
  cep: maskCepInput,
  phone: maskPhoneInput,
};

export type MaskedInputProps = Omit<InputProps, "value" | "onChange" | "defaultValue"> & {
  mask: MaskName;
  defaultValue?: string;
  /**
   * Valor controlado, mascarado na exibição. Só use junto com `onValueChange` —
   * sem ele o campo fica travado.
   */
  value?: string;
  /** Recebe os dígitos limpos e o texto já mascarado, nessa ordem. */
  onValueChange?: (digits: string, masked: string) => void;
};

/**
 * Campo com máscara progressiva. Mantém o valor formatado na tela e envia o
 * mesmo texto no `FormData` — a limpeza para dígitos acontece na Server Action,
 * que é quem fala com a API.
 *
 * <p>
 * Funciona nos dois modos. Sem `value`, guarda o próprio texto e basta soltar
 * no formulário. Com `value`, quem chama manda — é o que permite preencher o
 * campo a partir de uma consulta externa, coisa que um campo dono do próprio
 * estado não aceitaria de fora.
 */
export function MaskedInput({
  mask,
  defaultValue = "",
  value,
  onValueChange,
  ...props
}: MaskedInputProps) {
  const applyMask = MASKS[mask];
  const [internal, setInternal] = useState(() => applyMask(defaultValue));
  const controlled = value !== undefined;

  const handleChange = (event: ChangeEvent<HTMLInputElement>) => {
    const masked = applyMask(event.target.value);
    if (!controlled) setInternal(masked);
    onValueChange?.(masked.replace(/\D/g, ""), masked);
  };

  return (
    <Input
      {...props}
      value={controlled ? applyMask(value) : internal}
      onChange={handleChange}
      inputMode="numeric"
    />
  );
}
