"use client";

import { Badge, Button, DetailList, Modal } from "@/components/ui";
import type { DetailItem } from "@/components/ui";
import { formatLongDate } from "@/lib/format";
import type { Chair } from "@/features/chairs/types/chair.types";

export type ViewChairModalProps = {
  /** `null` mantém o modal fechado — o pai guarda a linha selecionada. */
  chair: Chair | null;
  onClose: () => void;
  /** Leva o mesmo registro para a edição, sem fechar e reabrir manualmente. */
  onEdit: (chair: Chair) => void;
};

export function ViewChairModal({ chair, onClose, onEdit }: ViewChairModalProps) {
  const items: DetailItem[] = chair
    ? [
        { label: "Nome", value: chair.name },
        {
          label: "Conexão",
          value: (
            <Badge tone={chair.online ? "success" : "warning"}>
              {chair.online ? "Online" : "Offline"}
            </Badge>
          ),
        },
        { label: "MAC address", value: chair.macAddress },
        { label: "Endereço", value: chair.ipAddress ? `${chair.ipAddress}:${chair.port}` : "—" },
        {
          label: "Situação",
          value: (
            <Badge tone={chair.active ? "success" : "neutral"}>
              {chair.active ? "Ativa" : "Inativa"}
            </Badge>
          ),
        },
        { label: "Último sinal", value: formatLongDate(chair.lastSeenAt) },
        { label: "Cadastrada em", value: formatLongDate(chair.createdAt), full: true },
      ]
    : [];

  return (
    <Modal
      open={Boolean(chair)}
      onClose={onClose}
      size="sm"
      title="Detalhes da cadeira"
      description="O MAC identifica o dispositivo; o IP é apenas o endereço atual na rede."
      footer={
        <>
          <Button variant="ghost" size="sm" onClick={onClose}>
            Fechar
          </Button>
          {chair && (
            <Button size="sm" onClick={() => onEdit(chair)}>
              Editar
            </Button>
          )}
        </>
      }
    >
      {chair && <DetailList items={items} />}
    </Modal>
  );
}
