"use client";

import { useTransition } from "react";
import { RowAction, useToast } from "@/components/ui";
import { pushChairNetworkAction } from "@/features/chairs/actions/chair.actions";
import type { Chair } from "@/features/chairs/types/chair.types";

export type PushNetworkActionProps = {
  chair: Chair;
  /** Disparado depois do envio — a tabela recarrega para mostrar a data nova. */
  onPushed: () => void;
};

/**
 * Grava a rede da empresa na memória do ESP32.
 *
 * <p>
 * Sem confirmação: reenviar a mesma configuração é inofensivo — o firmware
 * compara com o que já está na NVS e só reconecta se algo mudou. O que dói é o
 * contrário, esquecer uma cadeira depois de trocar a senha do Wi-Fi.
 */
export function PushNetworkAction({ chair, onPushed }: PushNetworkActionProps) {
  const [pending, startTransition] = useTransition();
  const toast = useToast();

  const push = () => {
    startTransition(async () => {
      const result = await pushChairNetworkAction(chair.id);

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
      label="Enviar configuração de rede"
      icon="wrench"
      disabled={pending}
      onClick={push}
    />
  );
}
