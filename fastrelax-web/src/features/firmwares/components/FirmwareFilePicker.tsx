"use client";

import { useRef, useState } from "react";
import { Button, Icon } from "@/components/ui";

export type FirmwareFilePickerProps = {
  /** Arquivo escolhido, ou `null` enquanto não houver. */
  file: File | null;
  onChange: (file: File | null) => void;
  disabled?: boolean;
};

const ACCEPT = ".bin,.hex";

/**
 * Escolha do binário antes de a versão existir.
 *
 * <p>
 * O upload é por id, e o id só nasce no insert — então aqui o arquivo apenas
 * fica retido no cliente, e o modal o envia depois de publicar. É o que permite
 * cadastrar e anexar num gesto só, em vez de obrigar a abrir os detalhes em
 * seguida.
 *
 * <p>
 * Nada de nome, tamanho ou hash digitado: os três saem do próprio arquivo no
 * servidor. O SHA-256 é o que o ESP32 confere antes de gravar, e aceitar um
 * valor vindo do cliente tornaria a checagem decorativa.
 */
export function FirmwareFilePicker({ file, onChange, disabled }: FirmwareFilePickerProps) {
  const input = useRef<HTMLInputElement>(null);
  const [dragging, setDragging] = useState(false);

  const pick = (picked: File | null) => {
    onChange(picked);
    // Limpa o input para reescolher o mesmo arquivo depois de remover — sem
    // isso o `change` não dispara na segunda vez.
    if (input.current) input.current.value = "";
  };

  return (
    <div className="flex flex-col gap-2 border-t border-line pt-4">
      <p className="text-xs font-semibold uppercase tracking-[0.14em] text-ink-muted">
        Binário (opcional)
      </p>

      <input
        ref={input}
        type="file"
        accept={ACCEPT}
        className="hidden"
        onChange={(event) => pick(event.target.files?.[0] ?? null)}
      />

      {file ? (
        <div className="flex items-center gap-3 rounded-control border border-line p-3">
          <Icon name="sheet" className="h-4 w-4 shrink-0 text-ink-muted" />
          <span className="flex min-w-0 flex-1 flex-col">
            <span className="truncate text-sm text-ink-primary">{file.name}</span>
            <span className="text-xs text-ink-tertiary">{formatBytes(file.size)}</span>
          </span>
          <button
            type="button"
            disabled={disabled}
            onClick={() => pick(null)}
            className="text-xs font-semibold text-error-400 underline underline-offset-2"
          >
            Remover
          </button>
        </div>
      ) : (
        <div
          onDragOver={(event) => {
            event.preventDefault();
            setDragging(true);
          }}
          onDragLeave={() => setDragging(false)}
          onDrop={(event) => {
            event.preventDefault();
            setDragging(false);
            pick(event.dataTransfer.files?.[0] ?? null);
          }}
          className={
            "flex flex-col items-center gap-2 rounded-control border border-dashed p-5 text-center " +
            (dragging ? "border-accent bg-accent/5" : "border-line")
          }
        >
          <Icon name="upload" className="h-5 w-5 text-ink-muted" />
          <p className="text-xs text-ink-tertiary">
            Arraste o <strong>.bin</strong> ou <strong>.hex</strong> aqui
          </p>
          <Button
            type="button"
            variant="secondary"
            size="sm"
            disabled={disabled}
            onClick={() => input.current?.click()}
          >
            Escolher arquivo
          </Button>
          <p className="text-xs text-ink-muted">
            Dá para anexar depois, pelos detalhes da versão.
          </p>
        </div>
      )}
    </div>
  );
}

/** Tamanho legível: 1048576 → "1,0 MB". */
function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}
