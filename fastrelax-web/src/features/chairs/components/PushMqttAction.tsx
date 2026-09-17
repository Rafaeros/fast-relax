"use client";

import { useTransition } from "react";
import { RowAction, useToast } from "@/components/ui";
import { pushChairMqttAction } from "@/features/chairs/actions/chair.actions";
import type { Chair } from "@/features/chairs/types/chair.types";

export type PushMqttActionProps = {
  chair: Chair;
  /** Disparado depois do envio — a tabela recarrega para mostrar a data nova. */
  onPushed: () => void;
};

/**
 * Grava o broker MQTT (override desta cadeira, ou o padrão global) na memória
 * do ESP32.
 *
 * <p>
 * Espelha `PushNetworkAction`: sem confirmação, pelo mesmo motivo —
 * reenviar a mesma configuração é inofensivo, o firmware só reconecta se algo
 * mudou. O que dói é o contrário, esquecer uma cadeira com override depois de
 * trocar a senha do broker dela.
 */
export function PushMqttAction({ chair, onPushed }: PushMqttActionProps) {
  const [pending, startTransition] = useTransition();
  const toast = useToast();

  const push = () => {
    startTransition(async () => {
      const result = await pushChairMqttAction(chair.id);

      if (result.ok) {
        onPushed();
        toast.success(result.message);
      } else {
        toast.error(result.message);
      }
    });
  };

  return (
    <RowAction
      label="Enviar configuração de MQTT"
      icon="wrench"
      disabled={pending}
      onClick={push}
    />
  );
}
