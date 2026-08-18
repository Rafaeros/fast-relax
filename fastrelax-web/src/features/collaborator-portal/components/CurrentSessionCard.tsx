"use client";

import { useTransition } from "react";
import Link from "next/link";
import type { Route } from "next";
import { Badge, Button, ButtonLink, Card, Icon, useToast } from "@/components/ui";
import { formatSessionDate, formatTimeRange } from "@/features/collaborator-portal/lib/format";
import {
  cancelSessionAction,
  finishSessionAction,
  startSessionAction,
} from "@/features/collaborator-portal/actions/portal.actions";
import type { CollaboratorSession } from "@/features/collaborator-portal/types/portal.types";

export type CurrentSessionCardProps = {
  /** `null` quando não há sessão em aberto — o cartão vira convite para agendar. */
  session: CollaboratorSession | null;
};

/**
 * Cartão de destaque da home: a próxima sessão, ou o convite para marcar uma.
 *
 * <p>
 * É o único lugar do app onde a sessão é acionada, então concentra as três
 * ações possíveis (iniciar, finalizar, cancelar) conforme o estado atual.
 */
export function CurrentSessionCard({ session }: CurrentSessionCardProps) {
  const [pending, startTransition] = useTransition();
  const { success, error } = useToast();

  const run = (action: () => Promise<{ ok: boolean; message: string }>) => {
    startTransition(async () => {
      // O cartão se redesenha com o novo estado da sessão logo depois da ação,
      // então o aviso vai para o toast em vez de disputar espaço aqui dentro.
      const result = await action();
      if (result.ok) {
        success(result.message);
      } else {
        error(result.message);
      }
    });
  };

  if (!session) {
    return (
      <Card padding="lg" className="flex flex-col gap-4">
        <div className="flex items-center gap-3">
          <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-surface-hover">
            <Icon name="calendar" className="h-5 w-5 text-ink-muted" />
          </span>
          <div>
            <p className="text-sm font-semibold text-ink-primary">Nenhuma sessão marcada</p>
            <p className="text-xs text-ink-tertiary">
              Escolha um horário dentro da sua janela permitida.
            </p>
          </div>
        </div>

        <ButtonLink href={"/colaborador/agenda" as Route} size="md" fullWidth>
          Agendar massagem
        </ButtonLink>
      </Card>
    );
  }

  const started = session.status === "STARTED";

  return (
    // Gradiente da marca no cartão principal: é a informação que o colaborador
    // abre o app para ver, e o contraste com os cartões neutros a destaca.
    <Card
      padding="lg"
      className="flex flex-col gap-5 border-accent/30 bg-gradient-to-br from-accent-strong/25 via-surface-card to-surface-card"
    >
      <div className="flex items-start justify-between gap-3">
        <div className="flex flex-col gap-1">
          <span className="text-xs font-semibold uppercase tracking-[0.14em] text-accent-soft">
            {started ? "Sessão em andamento" : "Sua próxima massagem"}
          </span>
          <p className="font-display text-2xl text-ink-primary">
            {formatTimeRange(session.startTime, session.endTime)}
          </p>
        </div>
        <Badge tone={started ? "success" : "accent"}>{session.statusLabel}</Badge>
      </div>

      <dl className="flex flex-col gap-2 text-sm">
        <div className="flex items-center gap-2">
          <Icon name="calendar" className="h-4 w-4 shrink-0 text-ink-muted" />
          <dt className="sr-only">Data</dt>
          <dd className="text-ink-secondary">{formatSessionDate(session.sessionDate)}</dd>
        </div>
        {session.chairName && (
          <div className="flex items-center gap-2">
            <Icon name="chair" className="h-4 w-4 shrink-0 text-ink-muted" />
            <dt className="sr-only">Cadeira</dt>
            <dd className="text-ink-secondary">{session.chairName}</dd>
          </div>
        )}
      </dl>

      <div className="flex flex-col gap-2 sm:flex-row">
        {started ? (
          <Button
            size="md"
            fullWidth
            disabled={pending}
            onClick={() => run(() => finishSessionAction(session.id))}
            leadingIcon={<Icon name="check" className="h-4 w-4" />}
          >
            {pending ? "Finalizando..." : "Finalizar sessão"}
          </Button>
        ) : (
          <Button
            size="md"
            fullWidth
            disabled={pending}
            onClick={() => run(() => startSessionAction(session.id))}
            leadingIcon={<Icon name="play" className="h-4 w-4" />}
          >
            {pending ? "Iniciando..." : "Iniciar agora"}
          </Button>
        )}

        {!started && (
          <Button
            variant="secondary"
            size="md"
            fullWidth
            disabled={pending}
            onClick={() => run(() => cancelSessionAction(session.id))}
          >
            Cancelar
          </Button>
        )}
      </div>

      {!started && (
        <p className="text-center text-xs text-ink-tertiary">
          O botão iniciar libera a cadeira no horário marcado.{" "}
          <Link href={"/colaborador/agenda" as Route} className="text-accent-soft underline">
            Ver outros horários
          </Link>
        </p>
      )}
    </Card>
  );
}
