"use client";

import { useTransition } from "react";
import { Badge, Button, DetailList, Icon, Modal, useToast } from "@/components/ui";
import type { DetailItem } from "@/components/ui";
import { formatLongDate } from "@/lib/format";
import { testChairRelayAction } from "@/features/chairs/actions/chair.actions";
import type { Chair } from "@/features/chairs/types/chair.types";

/** Mesmo default do backend; o relé desliga sozinho ao fim. */
const RELAY_TEST_SECONDS = 10;

export type ViewChairModalProps = {
  /** `null` mantém o modal fechado — o pai guarda a linha selecionada. */
  chair: Chair | null;
  onClose: () => void;
  /** Leva o mesmo registro para a edição, sem fechar e reabrir manualmente. */
  onEdit: (chair: Chair) => void;
  /** O teste de relé é restrito a ADMIN, tanto aqui quanto no backend. */
  isAdmin?: boolean;
};

export function ViewChairModal({ chair, onClose, onEdit, isAdmin = false }: ViewChairModalProps) {
  const [pending, startTransition] = useTransition();
  const { success, error } = useToast();

  const runRelayTest = () => {
    if (!chair) return;
    const chairId = chair.id;
    const chairName = chair.name;

    startTransition(async () => {
      // O toast vive fora do modal, então cita a cadeira pelo nome: o resultado
      // pode chegar depois de o técnico fechar o modal ou trocar de linha.
      const result = await testChairRelayAction(chairId, RELAY_TEST_SECONDS);
      const message = `${chairName}: ${result.message}`;
      if (result.ok) {
        success(message);
      } else {
        error(message);
      }
    });
  };

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
        { label: "Endereço", value: chair.ipAddress ? `:` : "—" },
        { label: "Firmware", value: chair.firmwareVersion ?? "Não informado" },
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
      {chair && (
        <div className="flex flex-col gap-5">
          <DetailList items={items} />

          {isAdmin && (
            <div className="flex flex-col gap-3 rounded-2xl border border-line p-4">
              <div>
                <p className="text-sm font-medium">Testar acionamento</p>
                <p className="mt-1 text-xs text-ink-secondary">
                  Liga o relé por {RELAY_TEST_SECONDS} segundos, sem criar sessão. Use para
                  conferir a fiação depois de instalar a cadeira.
                </p>
              </div>

              <Button
                variant="secondary"
                size="sm"
                onClick={runRelayTest}
                disabled={pending || !chair.ipAddress}
                leadingIcon={
                  pending ? (
                    <Icon name="loader" className="h-4 w-4 animate-spin" />
                  ) : (
                    <Icon name="play" className="h-4 w-4" />
                  )
                }
              >
                {pending ? "Acionando..." : "Acionar relé"}
              </Button>

              {!chair.ipAddress && (
                <p className="text-xs text-ink-secondary">
                  Disponível quando o dispositivo enviar o primeiro sinal.
                </p>
              )}
            </div>
          )}
        </div>
      )}
    </Modal>
  );
}
