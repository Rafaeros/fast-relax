"use client";

import { Alert, CopyField } from "@/components/ui";
import type { CredentialDelivery } from "@/features/authentication/types/auth.types";

export type CredentialDeliveryPanelProps = {
  delivery: CredentialDelivery;
  /** Complemento do texto: o que a pessoa faz a seguir, em cada contexto. */
  hint?: string;
};

/**
 * O que aconteceu com o acesso de uma conta recém-criada.
 *
 * <p>
 * Um componente para os três cadastros — usuário do painel, colaborador e
 * importação — porque a resposta da API é a mesma nos três. Duplicar isso faria
 * uma das telas continuar prometendo senha temporária depois de o convite virar
 * o caminho padrão.
 */
export function CredentialDeliveryPanel({ delivery, hint }: CredentialDeliveryPanelProps) {
  if (delivery.kind === "INVITE_SENT") {
    return (
      <div className="flex flex-col gap-3">
        <Alert tone="success" title="Convite enviado">
          Um link para definir a senha foi enviado para <strong>{delivery.email}</strong>. Ele vale
          por 48 horas.
        </Alert>
        {hint && <p className="text-xs text-ink-tertiary">{hint}</p>}
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-5">
      <Alert tone="warning" title="Senha exibida uma única vez">
        O sistema guarda apenas o hash. Perdendo este valor, resta redefinir a senha.
      </Alert>

      <CopyField label="Senha temporária" value={delivery.temporaryPassword ?? ""} />

      {hint && <p className="text-xs text-ink-tertiary">{hint}</p>}
    </div>
  );
}
