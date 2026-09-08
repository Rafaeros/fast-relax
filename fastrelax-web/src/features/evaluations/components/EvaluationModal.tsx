"use client";

import { useState, useTransition } from "react";
import { Button, Modal, Textarea, useToast } from "@/components/ui";
import { cn } from "@/lib/cn";
import { formatSessionDate, formatTimeRange } from "@/features/collaborator-portal/lib/format";
import type { CollaboratorSession } from "@/features/collaborator-portal/types/portal.types";
import { submitEvaluationAction } from "@/features/evaluations/actions/evaluation.actions";
import {
  EVALUATION_COMMENTS_MAX_LENGTH,
  EVALUATION_SCALE,
  type EvaluationScoreValue,
} from "@/features/evaluations/types/evaluation.types";

export type EvaluationModalProps = {
  open: boolean;
  /** Massagem avaliada — o modal só existe quando há uma. */
  session: CollaboratorSession;
  /** Fechar sem responder. A sessão continua pendente por 24h. */
  onClose: () => void;
  onSubmitted?: () => void;
};

/**
 * Modal de avaliação da massagem.
 *
 * <p>
 * Cinco rostos e um campo de texto opcional: é o máximo que se pede de alguém
 * que acabou de levantar da cadeira. Exigir comentário faria a maioria digitar
 * qualquer coisa para fechar a janela, e a nota é o dado que o RH acompanha.
 *
 * <p>
 * O botão de enviar só habilita depois da escolha da nota — sem seleção não há
 * o que mandar, e um envio recusado pelo backend seria uma viagem à toa.
 */
export function EvaluationModal({ open, session, onClose, onSubmitted }: EvaluationModalProps) {
  const [score, setScore] = useState<EvaluationScoreValue | null>(null);
  const [comments, setComments] = useState("");
  const [pending, startTransition] = useTransition();
  const { success, error } = useToast();

  const selected = EVALUATION_SCALE.find((item) => item.value === score);
  const remaining = EVALUATION_COMMENTS_MAX_LENGTH - comments.length;

  const submit = () => {
    if (!score) return;

    startTransition(async () => {
      const result = await submitEvaluationAction({ sessionId: session.id, score, comments });

      if (result.ok) {
        success(result.message);
        onSubmitted?.();
        onClose();
      } else {
        error(result.message);
      }
    });
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      // Enquanto envia, ESC e clique fora não fecham: a resposta já está a
      // caminho e sumir com a janela deixaria a pessoa sem saber se contou.
      dismissible={!pending}
      size="sm"
      title="Como foi a sua massagem?"
      description={`${formatSessionDate(session.sessionDate)} · ${formatTimeRange(
        session.startTime,
        session.endTime,
      )}${session.chairName ? ` · ${session.chairName}` : ""}`}
      footer={
        <>
          <Button variant="ghost" size="sm" disabled={pending} onClick={onClose}>
            Agora não
          </Button>
          <Button size="sm" disabled={pending || !score} onClick={submit}>
            {pending ? "Enviando..." : "Enviar avaliação"}
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-5">
        <fieldset className="flex flex-col gap-2">
          <legend className="sr-only">Nota da massagem</legend>

          {/* Os cinco rostos numa linha só, mesmo no celular: a escala é uma
              régua, e quebrá-la em duas linhas desfaz a leitura de pior para
              melhor. */}
          <div className="flex items-stretch justify-between gap-1">
            {EVALUATION_SCALE.map((item) => {
              const active = item.value === score;

              return (
                <button
                  key={item.value}
                  type="button"
                  aria-pressed={active}
                  aria-label={item.label}
                  disabled={pending}
                  onClick={() => setScore(item.value)}
                  className={cn(
                    "flex flex-1 flex-col items-center gap-1 rounded-control border px-1 py-3",
                    "transition-colors disabled:opacity-60",
                    active
                      ? "border-accent-strong bg-accent/10"
                      : "border-line bg-surface-card hover:bg-surface-hover",
                  )}
                >
                  <span
                    className={cn(
                      "text-2xl transition-transform",
                      active ? "scale-110" : "grayscale",
                    )}
                    aria-hidden
                  >
                    {item.emoji}
                  </span>
                  <span
                    className={cn(
                      "text-[0.625rem] font-semibold leading-tight",
                      active ? "text-accent-soft" : "text-ink-tertiary",
                    )}
                  >
                    {item.label}
                  </span>
                </button>
              );
            })}
          </div>

          {/* Espaço reservado mesmo sem escolha: sem isto o modal saltaria de
              altura no primeiro toque. */}
          <p className="min-h-4 text-center text-xs text-ink-tertiary">
            {selected ? `Você marcou "${selected.label}".` : "Toque em um rosto para avaliar."}
          </p>
        </fieldset>

        <Textarea
          name="comments"
          label="Quer contar mais alguma coisa? (opcional)"
          placeholder="O que ajudaria a melhorar a sua próxima massagem?"
          rows={3}
          maxLength={EVALUATION_COMMENTS_MAX_LENGTH}
          disabled={pending}
          value={comments}
          onChange={(event) => setComments(event.target.value)}
          hint={`${remaining} caracteres restantes`}
        />

        <p className="text-xs text-ink-tertiary">
          O RH da sua empresa vê esta avaliação junto do seu nome.
        </p>
      </div>
    </Modal>
  );
}
