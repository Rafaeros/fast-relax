"use client";

import { useState } from "react";
import { Icon, Input } from "@/components/ui";
import type { InputProps } from "@/components/ui";

export type PasswordFieldProps = Omit<InputProps, "type" | "trailing" | "leadingIcon">;

/**
 * Campo de senha com alternância de visibilidade.
 * Reaproveita o `Input` global — aqui mora só o comportamento de mostrar/ocultar.
 */
export function PasswordField(props: PasswordFieldProps) {
  const [visible, setVisible] = useState(false);

  return (
    <Input
      {...props}
      type={visible ? "text" : "password"}
      leadingIcon={<Icon name="lock" />}
      trailing={
        <button
          type="button"
          onClick={() => setVisible((current) => !current)}
          aria-label={visible ? "Ocultar senha" : "Mostrar senha"}
          aria-pressed={visible}
          className="rounded-control p-2 text-ink-tertiary transition-colors hover:text-ink-primary focus-visible:outline-none focus-visible:shadow-focus"
        >
          <Icon name={visible ? "eyeOff" : "eye"} className="h-4 w-4" />
        </button>
      }
    />
  );
}
