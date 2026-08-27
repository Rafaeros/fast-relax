"use client";

import { useState } from "react";
import { Button, DetailList, Modal } from "@/components/ui";
import type { DetailItem } from "@/components/ui";
import { formatLongDate } from "@/lib/format";
import { fetchFirmwareAction } from "@/features/firmwares/actions/firmware.actions";
import { FirmwareFilesManager } from "@/features/firmwares/components/FirmwareFilesManager";
import type { Firmware } from "@/features/firmwares/types/firmware.types";

export type ViewFirmwareModalProps = {
  /** `null` mantém o modal fechado — o pai guarda a linha selecionada. */
  firmware: Firmware | null;
  onClose: () => void;
  /** Leva o mesmo registro para a edição, sem fechar e reabrir manualmente. */
  onEdit: (firmware: Firmware) => void;
  /** Anexar e remover binários é da equipe da plataforma. */
  canManage?: boolean;
  /** Disparado quando um binário entra ou sai — a tabela recarrega por aqui. */
  onChanged: () => void;
};

/**
 * Detalhes da versão e os binários dela.
 *
 * <p>
 * É aqui que o arquivo é anexado, baixado e gravado no ESP32 — e não no
 * formulário de cadastro. O motivo é prático: o upload precisa do id da versão,
 * que só existe depois de publicada.
 */
export function ViewFirmwareModal({
  firmware,
  onClose,
  onEdit,
  canManage = false,
  onChanged,
}: ViewFirmwareModalProps) {
  // O registro chega capturado na abertura do modal. Anexar um binário muda a
  // lista no servidor, mas não este objeto — por isso a releitura. O id viaja
  // junto para que trocar de linha sem fechar descarte o refresh anterior por
  // comparação, sem precisar de efeito para limpar o estado.
  const [refreshed, setRefreshed] = useState<Firmware | null>(null);
  const current = refreshed && firmware && refreshed.id === firmware.id ? refreshed : firmware;

  const handleChanged = () => {
    // A tabela recarrega por fora; aqui só o registro em tela é reconciliado.
    onChanged();

    if (!firmware) return;
    void fetchFirmwareAction(firmware.id).then((fresh) => {
      if (fresh) setRefreshed(fresh);
    });
  };

  const items: DetailItem[] = current
    ? [
        { label: "Produto", value: current.productName },
        { label: "Versão", value: current.version },
        { label: "Publicado em", value: formatLongDate(current.releaseDate) },
        { label: "Binários", value: String(current.files.length) },
        { label: "Notas", value: current.releaseNotes || "—", full: true },
      ]
    : [];

  return (
    <Modal
      open={Boolean(firmware)}
      onClose={onClose}
      size="md"
      title="Detalhes do firmware"
      description="Anexe o binário, baixe ou grave direto na placa pela porta USB."
      footer={
        <>
          <Button variant="ghost" size="sm" onClick={onClose}>
            Fechar
          </Button>
          {firmware && (
            <Button size="sm" onClick={() => onEdit(firmware)}>
              Editar
            </Button>
          )}
        </>
      }
    >
      {current && (
        <div className="flex flex-col gap-5">
          <DetailList items={items} />

          <div className="border-t border-line pt-4">
            <FirmwareFilesManager
              firmware={current}
              canManage={canManage}
              onChanged={handleChanged}
            />
          </div>
        </div>
      )}
    </Modal>
  );
}
