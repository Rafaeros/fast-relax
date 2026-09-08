"use client";

import { Badge, DetailList, Modal } from "@/components/ui";
import { formatLongDate } from "@/lib/format";
import { formatSessionDate } from "@/features/collaborator-portal/lib/format";
import { scaleItemFor, type Evaluation } from "@/features/evaluations/types/evaluation.types";

export type ViewEvaluationModalProps = {
  /** `null` fecha o modal — um diálogo por tela, não um por linha. */
  evaluation: Evaluation | null;
  onClose: () => void;
};

/**
 * Avaliação inteira, com o comentário sem corte.
 *
 * <p>
 * Na tabela o comentário aparece truncado para a linha não crescer; é aqui que
 * o RH lê o texto completo, que costuma ser a parte que explica a nota.
 */
export function ViewEvaluationModal({ evaluation, onClose }: ViewEvaluationModalProps) {
  const scale = scaleItemFor(evaluation?.score);

  return (
    <Modal
      open={evaluation !== null}
      onClose={onClose}
      title="Avaliação da massagem"
      description={evaluation?.collaboratorName}
    >
      {evaluation && (
        <div className="flex flex-col gap-5">
          <div className="flex items-center gap-3 rounded-control border border-line bg-surface-hover p-4">
            <span className="text-3xl" aria-hidden>
              {scale?.emoji}
            </span>
            <div className="flex flex-col gap-1">
              <Badge tone={scale?.tone ?? "neutral"}>{evaluation.scoreLabel}</Badge>
              <span className="text-xs text-ink-tertiary">
                Nota {evaluation.score} de 5 · respondida em {formatLongDate(evaluation.evaluationDate)}
              </span>
            </div>
          </div>

          <DetailList
            items={[
              { label: "Colaborador", value: evaluation.collaboratorName },
              { label: "Departamento", value: evaluation.departmentName ?? "—" },
              { label: "Massagem", value: formatSessionDate(evaluation.sessionDate) },
              { label: "Cadeira", value: evaluation.chairName ?? "—" },
              {
                label: "Comentário",
                full: true,
                value: evaluation.comments ? (
                  // `whitespace-pre-line`: quem escreveu em linhas separadas
                  // continua sendo lido assim.
                  <span className="whitespace-pre-line text-ink-secondary">
                    {evaluation.comments}
                  </span>
                ) : (
                  <span className="text-ink-tertiary">Sem comentário.</span>
                ),
              },
            ]}
          />
        </div>
      )}
    </Modal>
  );
}
