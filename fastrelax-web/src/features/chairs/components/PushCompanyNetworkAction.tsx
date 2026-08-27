"use client";

import { useTransition } from "react";
import { RowAction, useToast } from "@/components/ui";
import { pushCompanyNetworkAction } from "@/features/chairs/actions/chair.actions";
import type { ChairNetworkResult } from "@/features/chairs/types/chair.types";

export type PushCompanyNetworkActionProps = {
  companyId: number;
  /** Sem rede cadastrada não há o que enviar; a ação some da linha. */
  wifiConfigured: boolean;
};

/**
 * Reenvia a rede da empresa para todas as cadeiras ativas dela.
 *
 * <p>
 * É o gesto de depois de trocar a senha do Wi-Fi: sem ele, cada cadeira teria
 * de ser reenviada uma a uma, e a esquecida só apareceria quando o AP antigo
 * saísse do ar — já offline, longe do teclado de quem trocou a senha.
 */
export function PushCompanyNetworkAction({
  companyId,
  wifiConfigured,
}: PushCompanyNetworkActionProps) {
  const [pending, startTransition] = useTransition();
  const toast = useToast();

  if (!wifiConfigured) return null;

  const push = () => {
    startTransition(async () => {
      const result = await pushCompanyNetworkAction(companyId);

      if (!result.ok) {
        toast.error(result.message);
        return;
      }

      const missed = result.results.filter((entry) => !entry.delivered);

      // Um envio em lote onde metade falhou ainda volta `ok`: o backend não
      // aborta por uma cadeira muda. Anunciar sucesso aqui esconderia as que
      // ficaram para trás, que são exatamente as que precisam de alguém.
      if (missed.length > 0) {
        toast.error(describeMissed(missed));
      } else {
        toast.success(result.message);
      }
    });
  };

  return (
    <RowAction
      label="Reenviar rede às cadeiras"
      icon="wrench"
      disabled={pending}
      onClick={push}
    />
  );
}

/** Nomeia as cadeiras que faltaram — sem elas o aviso não diz onde ir olhar. */
function describeMissed(missed: ChairNetworkResult[]): string {
  const names = missed.map((entry) => entry.chairName).join(", ");
  return missed.length === 1
    ? `A cadeira ${names} não recebeu a configuração.`
    : `${missed.length} cadeiras não receberam a configuração: ${names}.`;
}
