"use client";

import { useState, type ChangeEvent } from "react";
import { Input, type InputProps } from "@/components/ui/Input";
import { maskCpfInput, maskPhoneInput } from "@/lib/format";

export type MaskName = "cpf" | "phone";

const MASKS: Record<MaskName, (value: string) => string> = {
  cpf: maskCpfInput,
  phone: maskPhoneInput,
};

export type MaskedInputProps = Omit<InputProps, "value" | "onChange" | "defaultValue"> & {
  mask: MaskName;
  defaultValue?: string;
};

/**
 * Campo com máscara progressiva. Mantém o valor formatado na tela e envia o
 * mesmo texto no `FormData` — a limpeza para dígitos acontece na Server Action,
 * que é quem fala com a API.
 */
export function MaskedInput({ mask, defaultValue = "", ...props }: MaskedInputProps) {
  const applyMask = MASKS[mask];
  const [value, setValue] = useState(() => applyMask(defaultValue));

  const handleChange = (event: ChangeEvent<HTMLInputElement>) => {
    setValue(applyMask(event.target.value));
  };

  return <Input {...props} value={value} onChange={handleChange} inputMode="numeric" />;
}
