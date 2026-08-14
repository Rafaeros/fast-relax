"use client";

import { useRef, useState } from "react";
import { cn } from "@/lib/cn";
import { Icon } from "@/components/ui/Icon";
import { IconButton } from "@/components/ui/IconButton";

export type CopyFieldProps = {
  value: string;
  label?: string;
  /** Fonte monoespaçada — para senhas, tokens e códigos. */
  mono?: boolean;
  className?: string;
};

/**
 * Valor somente leitura com botão de copiar.
 * Usado para dados exibidos uma única vez (senha temporária), onde digitar de
 * novo é fonte garantida de erro.
 */
export function CopyField({ value, label, mono = true, className }: CopyFieldProps) {
  const [copied, setCopied] = useState(false);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(value);
    } catch {
      // Sem permissão de área de transferência: o valor segue visível para
      // seleção manual, então não vale interromper o fluxo com erro.
      return;
    }

    setCopied(true);
    if (timerRef.current) clearTimeout(timerRef.current);
    timerRef.current = setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className={cn("flex flex-col gap-1.5", className)}>
      {label && (
        <span className="text-xs font-semibold tracking-wide text-ink-secondary">{label}</span>
      )}

      <div className="flex items-center gap-2 rounded-control border border-line bg-bg-900 px-3 py-2">
        <span
          className={cn(
            "min-w-0 flex-1 truncate text-sm text-ink-primary select-all",
            mono && "font-mono tracking-wide",
          )}
        >
          {value}
        </span>

        <IconButton
          label={copied ? "Copiado" : "Copiar"}
          tone={copied ? "accent" : "neutral"}
          onClick={copy}
          icon={<Icon name={copied ? "check" : "copy"} className="h-4 w-4" />}
        />
      </div>
    </div>
  );
}
