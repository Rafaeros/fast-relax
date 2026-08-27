"use client";

import { useEffect, useRef, useState, useTransition } from "react";
import { Button, Icon, useToast } from "@/components/ui";
import { cn } from "@/lib/cn";
import {
  deleteFirmwareFileAction,
  uploadFirmwareFileAction,
} from "@/features/firmwares/actions/firmware.actions";
import {
  resolveFlashUnavailableReason,
  flashFirmware,
  requestSerialPort,
  type FlashProgress,
} from "@/features/firmwares/lib/esp-flasher";
import type { Firmware, FirmwareFile } from "@/features/firmwares/types/firmware.types";

export type FirmwareFilesManagerProps = {
  firmware: Firmware;
  /** Anexar e remover são da equipe da plataforma; baixar e gravar, não. */
  canManage?: boolean;
  /** Disparado após anexar ou remover — a tabela recarrega a partir daqui. */
  onChanged: () => void;
};

const ACCEPT = ".bin,.hex";

/**
 * Binários de uma versão: anexar, baixar, remover e gravar no ESP32.
 *
 * <p>
 * A gravação acontece no próprio navegador, pela Web Serial — sem esptool
 * instalado na máquina e sem servidor no meio. Ela só aparece quando o
 * navegador oferece a API, e só para arquivos `.bin`: o esptool não entende
 * Intel HEX.
 */
export function FirmwareFilesManager({
  firmware,
  canManage = false,
  onChanged,
}: FirmwareFilesManagerProps) {
  const [pending, startTransition] = useTransition();
  const [flashing, setFlashing] = useState<number | null>(null);
  const [progress, setProgress] = useState<FlashProgress | null>(null);
  const [flashSupport, setFlashSupport] = useState<string | null>(null);
  const fileInput = useRef<HTMLInputElement>(null);
  const { success, error } = useToast();

  useEffect(() => {
    // A checagem só existe no cliente (depende de `window.isSecureContext`), e
    // chega por callback para não renderizar em cascata dentro do efeito.
    let active = true;

    resolveFlashUnavailableReason().then((reason) => {
      if (active) setFlashSupport(reason);
    });

    return () => {
      active = false;
    };
  }, []);

  const upload = (file: File) => {
    const body = new FormData();
    body.append("file", file);

    startTransition(async () => {
      const result = await uploadFirmwareFileAction(firmware.id, body);

      if (result.ok) {
        success(result.message);
        onChanged();
      } else {
        error(result.message);
      }

      // Limpa o input para o mesmo arquivo poder ser escolhido de novo depois
      // de uma falha — sem isso o `change` não dispara na segunda vez.
      if (fileInput.current) fileInput.current.value = "";
    });
  };

  const remove = (file: FirmwareFile) => {
    startTransition(async () => {
      const result = await deleteFirmwareFileAction(firmware.id, file.id);

      if (result.ok) {
        success(result.message);
        onChanged();
      } else {
        error(result.message);
      }
    });
  };

  const flash = async (file: FirmwareFile) => {
    const port = await requestSerialPort();
    // Seletor fechado sem escolher: silêncio é a resposta certa.
    if (!port) return;

    setFlashing(file.id);
    setProgress({ percent: 0, message: "Baixando o binário..." });

    try {
      const response = await fetch(
        `/api/firmwares/${firmware.id}/files/${file.id}?nome=${encodeURIComponent(file.fileName)}`,
      );
      if (!response.ok) throw new Error("Não foi possível baixar o binário desta versão.");

      await flashFirmware(port, await response.arrayBuffer(), setProgress);
      success(`${file.fileName} gravado com sucesso.`);
    } catch (cause) {
      error(cause instanceof Error ? cause.message : "Falha ao gravar o firmware.");
    } finally {
      setFlashing(null);
      setProgress(null);
    }
  };

  const busy = pending || flashing !== null;

  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center justify-between gap-3">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.14em] text-ink-muted">
            Binários
          </p>
          <p className="text-xs text-ink-tertiary">
            O SHA-256 é calculado no envio e é o que o ESP32 confere antes de gravar.
          </p>
        </div>

        {canManage && (
          <>
            <input
              ref={fileInput}
              type="file"
              accept={ACCEPT}
              className="hidden"
              onChange={(event) => {
                const file = event.target.files?.[0];
                if (file) upload(file);
              }}
            />
            <Button
              type="button"
              variant="secondary"
              size="sm"
              disabled={busy}
              onClick={() => fileInput.current?.click()}
              leadingIcon={
                pending ? (
                  <Icon name="loader" className="h-4 w-4 animate-spin" />
                ) : (
                  <Icon name="upload" className="h-4 w-4" />
                )
              }
            >
              Anexar
            </Button>
          </>
        )}
      </div>

      {firmware.files.length === 0 ? (
        <p className="rounded-control border border-dashed border-line p-4 text-center text-xs text-ink-tertiary">
          Nenhum binário anexado. Envie o .bin gerado pelo PlatformIO para liberar a gravação.
        </p>
      ) : (
        <ul className="flex flex-col divide-y divide-line rounded-control border border-line">
          {firmware.files.map((file) => (
            <li key={file.id} className="flex flex-col gap-3 p-3">
              <div className="flex items-start gap-3">
                <Icon name="sheet" className="mt-0.5 h-4 w-4 shrink-0 text-ink-muted" />
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm text-ink-primary">{file.fileName}</p>
                  <p className="text-xs tabular-nums text-ink-tertiary">
                    {formatBytes(file.fileSize)}
                  </p>
                  {/* Hash inteiro, quebrando linha: é conferido caractere a
                      caractere quando algo dá errado em campo. */}
                  <p className="mt-1 break-all font-mono text-[0.625rem] leading-tight text-ink-muted">
                    {file.fileHash}
                  </p>
                </div>
              </div>

              <div className="flex flex-wrap items-center gap-2">
                <a
                  href={`/api/firmwares/${firmware.id}/files/${file.id}?nome=${encodeURIComponent(file.fileName)}`}
                  className="inline-flex items-center gap-1.5 text-xs font-semibold text-accent-soft underline underline-offset-2"
                >
                  <Icon name="download" className="h-3.5 w-3.5" />
                  Baixar
                </a>

                {file.flashable && !flashSupport && (
                  <Button
                    type="button"
                    variant="secondary"
                    size="sm"
                    disabled={busy}
                    onClick={() => flash(file)}
                    leadingIcon={
                      flashing === file.id ? (
                        <Icon name="loader" className="h-4 w-4 animate-spin" />
                      ) : (
                        <Icon name="wrench" className="h-4 w-4" />
                      )
                    }
                  >
                    Gravar no ESP32
                  </Button>
                )}

                {canManage && (
                  <button
                    type="button"
                    disabled={busy}
                    onClick={() => remove(file)}
                    className="ml-auto text-xs font-semibold text-error-400 underline underline-offset-2"
                  >
                    Remover
                  </button>
                )}
              </div>

              {!file.flashable && (
                <p className="text-xs text-ink-tertiary">
                  Intel HEX não pode ser gravado no ESP32 — disponível para download apenas.
                </p>
              )}

              {flashing === file.id && progress && (
                <div className="flex flex-col gap-1">
                  <div className="h-1.5 w-full overflow-hidden rounded-full bg-surface-hover">
                    <div
                      className={cn("h-full bg-accent transition-[width] duration-200")}
                      style={{ width: `${progress.percent}%` }}
                    />
                  </div>
                  <p className="text-xs text-ink-tertiary">
                    {progress.message}
                    {progress.percent > 0 && ` (${progress.percent}%)`}
                  </p>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}

      {flashSupport && firmware.files.some((file) => file.flashable) && (
        <p className="text-xs text-ink-tertiary">{flashSupport}</p>
      )}
    </div>
  );
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}
