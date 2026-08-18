"use client";

import { useEffect, useState, useTransition } from "react";
import { Badge, Button, Card, Icon, useToast } from "@/components/ui";
import {
  getVapidPublicKeyAction,
  registerWebPushAction,
  unregisterWebPushAction,
} from "@/features/notifications/actions/notification.actions";
import {
  resolvePushStatus,
  subscribeToPush,
  unsubscribeFromPush,
  type PushStatus,
} from "@/features/notifications/lib/push-client";

type State = { kind: "checking" } | PushStatus;

/**
 * Liga e desliga as notificações deste navegador.
 *
 * <p>
 * A inscrição é por navegador, não por conta: o mesmo colaborador precisa
 * autorizar de novo em outro aparelho, e é por isso que o texto fala em "neste
 * navegador" em vez de "na sua conta".
 */
export function PushToggle() {
  const [state, setState] = useState<State>({ kind: "checking" });
  const [pending, startTransition] = useTransition();
  const { success, error } = useToast();

  useEffect(() => {
    // A checagem só existe no cliente (depende de `window`), então o primeiro
    // render sai como "checking" no servidor e no cliente — sem divergência de
    // hidratação.
    let active = true;

    resolvePushStatus().then((status) => {
      if (active) setState(status);
    });

    return () => {
      active = false;
    };
  }, []);

  const enable = () => {
    startTransition(async () => {
      try {
        const publicKey = await getVapidPublicKeyAction();
        if (!publicKey) {
          error("O servidor está sem as chaves de push configuradas. Procure o TI.");
          return;
        }

        const subscription = await subscribeToPush(publicKey);
        const result = await registerWebPushAction(subscription);

        if (!result.ok) {
          error(result.message);
          return;
        }

        setState({ kind: "on" });
        success("Notificações ativadas neste navegador.");
      } catch (cause) {
        error(cause instanceof Error ? cause.message : "Não foi possível ativar as notificações.");
      }
    });
  };

  const disable = () => {
    startTransition(async () => {
      try {
        const endpoint = await unsubscribeFromPush();
        // Desinscreve no navegador antes de avisar a API: se a ordem se
        // invertesse e a chamada falhasse, o registro ficaria ativo no servidor
        // apontando para uma inscrição que já não existe.
        if (endpoint) await unregisterWebPushAction(endpoint);

        setState({ kind: "off" });
        success("Notificações desativadas neste navegador.");
      } catch {
        error("Não foi possível desativar as notificações.");
      }
    });
  };

  return (
    <Card padding="lg" className="flex flex-col gap-4">
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-3">
          <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-surface-hover">
            <Icon name="bell" className="h-5 w-5 text-ink-muted" />
          </span>
          <div>
            <p className="text-sm font-semibold text-ink-primary">Notificações</p>
            <p className="text-xs text-ink-tertiary">
              Avisos de agendamento e o lembrete antes da sua massagem.
            </p>
          </div>
        </div>

        {state.kind === "on" && <Badge tone="success">Ativas</Badge>}
      </div>

      {state.kind === "unsupported" ? (
        <p className="text-xs text-ink-tertiary">{state.reason}</p>
      ) : (
        <Button
          variant={state.kind === "on" ? "secondary" : "primary"}
          size="md"
          fullWidth
          disabled={pending || state.kind === "checking"}
          onClick={state.kind === "on" ? disable : enable}
          leadingIcon={
            pending ? (
              <Icon name="loader" className="h-4 w-4 animate-spin" />
            ) : (
              <Icon name="bell" className="h-4 w-4" />
            )
          }
        >
          {state.kind === "on" ? "Desativar neste navegador" : "Ativar neste navegador"}
        </Button>
      )}
    </Card>
  );
}
