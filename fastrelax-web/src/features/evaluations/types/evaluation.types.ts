import type { BadgeTone } from "@/components/ui";
import type { PageParams } from "@/lib/api/pagination.types";

/** Espelha `EvaluationScore` do fastrelax-api. */
export type EvaluationScoreValue = 1 | 2 | 3 | 4 | 5;

export type EvaluationScaleItem = {
  value: EvaluationScoreValue;
  /** O rosto que a pessoa toca; o backend só conhece o número. */
  emoji: string;
  label: string;
  tone: BadgeTone;
};

/**
 * A escala, do pior para o melhor.
 *
 * <p>
 * Os rótulos repetem os do backend de propósito: a nota antiga continua legível
 * na tela do RH mesmo que a redação mude aqui, porque a API manda o
 * `scoreLabel` junto de cada avaliação. Esta lista é para quem ainda vai
 * responder.
 */
export const EVALUATION_SCALE: EvaluationScaleItem[] = [
  { value: 1, emoji: "😖", label: "Péssimo", tone: "error" },
  { value: 2, emoji: "🙁", label: "Ruim", tone: "warning" },
  { value: 3, emoji: "😐", label: "Regular", tone: "neutral" },
  { value: 4, emoji: "🙂", label: "Bom", tone: "info" },
  { value: 5, emoji: "😄", label: "Excelente", tone: "success" },
];

export function scaleItemFor(score: number | null | undefined): EvaluationScaleItem | undefined {
  return EVALUATION_SCALE.find((item) => item.value === score);
}

/** Espelha `EvaluationResponseDTO`. */
export type Evaluation = {
  id: number;
  collaboratorId: number;
  collaboratorName: string;
  departmentName: string | null;
  sessionId: number;
  /** "YYYY-MM-DD" — o dia da massagem, não o da resposta. */
  sessionDate: string;
  chairName: string | null;
  score: EvaluationScoreValue;
  /** Mesma nota em português, enviada pela API para exibição. */
  scoreLabel: string;
  comments: string | null;
  evaluationDate: string;
};

/** Espelha `EvaluationSummaryDTO.ScoreCountDTO`. */
export type ScoreCount = {
  score: EvaluationScoreValue;
  label: string;
  total: number;
};

/** Espelha `EvaluationSummaryDTO`. */
export type EvaluationSummary = {
  total: number;
  /** `null` quando ninguém avaliou no período — não zero, que seria uma nota. */
  average: number | null;
  sessionsDone: number;
  /** Percentual de massagens concluídas que receberam nota. */
  responseRate: number | null;
  distribution: ScoreCount[];
};

/** Espelha `EvaluationFilterDTO`. */
export type EvaluationFilter = {
  score?: EvaluationScoreValue;
  collaboratorId?: number;
  departmentId?: number;
  /** "YYYY-MM-DD", pela data da avaliação. */
  from?: string;
  to?: string;
};

export type ListEvaluationsParams = EvaluationFilter & PageParams;

/** Espelha `CreateEvaluationDTO` — sem `collaboratorId`: quem avalia é quem está logado. */
export type CreateEvaluationInput = {
  sessionId: number;
  score: EvaluationScoreValue;
  comments?: string;
};

/** Resultado das ações de avaliação, no mesmo formato do portal. */
export type EvaluationActionResult = {
  ok: boolean;
  message: string;
};

/** Teto do comentário, igual ao `@Size(max = 500)` do backend. */
export const EVALUATION_COMMENTS_MAX_LENGTH = 500;
