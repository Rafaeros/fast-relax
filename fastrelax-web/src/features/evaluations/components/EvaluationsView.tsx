"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import {
  Badge,
  InfiniteDataTable,
  RowActions,
  TableIdentity,
  TableToolbar,
  ViewAction,
} from "@/components/ui";
import type { DataTableColumn } from "@/components/ui";
import type { PageSlice } from "@/lib/api/pagination.types";
import { formatLongDate } from "@/lib/format";
import { formatSessionDate } from "@/features/collaborator-portal/lib/format";
import {
  fetchEvaluationSummaryAction,
  fetchEvaluationsPage,
} from "@/features/evaluations/actions/evaluation.actions";
import { EvaluationsFilterModal } from "@/features/evaluations/components/EvaluationsFilterModal";
import { EvaluationsSummary } from "@/features/evaluations/components/EvaluationsSummary";
import { ViewEvaluationModal } from "@/features/evaluations/components/ViewEvaluationModal";
import {
  scaleItemFor,
  type Evaluation,
  type EvaluationFilter,
  type EvaluationSummary,
} from "@/features/evaluations/types/evaluation.types";

export type EvaluationsViewProps = {
  initialSlice: PageSlice<Evaluation>;
  initialSummary: EvaluationSummary | null;
  departments: { id: number; name: string }[];
};

/**
 * Tela de avaliações do RH: os números do período em cima, as respostas embaixo.
 *
 * <p>
 * O filtro é um só para as duas partes. Deixar o resumo preso ao carregamento
 * inicial faria a média continuar respondendo por tudo enquanto a lista já
 * mostraria só as notas baixas — dois números na mesma tela dizendo coisas
 * diferentes.
 */
export function EvaluationsView({
  initialSlice,
  initialSummary,
  departments,
}: EvaluationsViewProps) {
  const [filter, setFilter] = useState<EvaluationFilter>({});
  const [summary, setSummary] = useState<EvaluationSummary | null>(initialSummary);
  const [viewing, setViewing] = useState<Evaluation | null>(null);

  const [summaryKey, setSummaryKey] = useState("{}");

  const filterKey = JSON.stringify(filter);
  const filtering = Object.keys(filter).length > 0;

  // Ajuste de estado durante o render, o mesmo padrão da tabela: limpar o
  // filtro volta na hora ao resumo que veio do servidor, sem uma ida à API para
  // chegar a um número que já está em mãos. Com filtro, o resumo anterior fica
  // em tela até o novo chegar — piscar vazio a cada troca seria pior que ficar
  // um instante defasado.
  if (filterKey !== summaryKey) {
    setSummaryKey(filterKey);
    if (!filtering) setSummary(initialSummary);
  }

  useEffect(() => {
    if (!filtering) return;

    let current = true;
    fetchEvaluationSummaryAction(filter).then((result) => {
      // Filtro trocado durante a busca: a resposta antiga não pode sobrescrever
      // a nova.
      if (current) setSummary(result);
    });

    return () => {
      current = false;
    };
    // `filterKey` representa o filtro inteiro; compará-lo por identidade
    // dispararia a cada render.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filterKey, filtering]);

  // Identidade estável por filtro: sem isso o observer da rolagem infinita
  // seria recriado a cada render.
  const loadFilteredPage = useCallback(
    (page: number) => fetchEvaluationsPage(page, filter),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [filterKey],
  );

  const columns = useMemo<DataTableColumn<Evaluation>[]>(
    () => [
      {
        id: "collaborator",
        header: "Colaborador",
        cell: (row) => (
          <TableIdentity name={row.collaboratorName} secondary={row.departmentName ?? undefined} />
        ),
      },
      {
        id: "score",
        header: "Nota",
        cell: (row) => {
          const scale = scaleItemFor(row.score);

          return (
            <span className="flex items-center gap-2">
              <span className="text-lg" aria-hidden>
                {scale?.emoji}
              </span>
              <Badge tone={scale?.tone ?? "neutral"}>{row.scoreLabel}</Badge>
            </span>
          );
        },
      },
      {
        id: "comments",
        header: "Comentário",
        hideOnMobile: true,
        cell: (row) =>
          row.comments ? (
            // Uma linha por avaliação: o texto inteiro fica no modal, e deixar
            // a célula crescer transformaria a tabela numa lista de parágrafos.
            <span className="block max-w-md truncate text-ink-secondary" title={row.comments}>
              {row.comments}
            </span>
          ) : (
            <span className="text-ink-tertiary">—</span>
          ),
      },
      {
        id: "sessionDate",
        header: "Massagem",
        hideOnMobile: true,
        cell: (row) => (
          <span className="flex flex-col">
            <span className="text-ink-secondary">{formatSessionDate(row.sessionDate)}</span>
            {row.chairName && <span className="text-xs text-ink-tertiary">{row.chairName}</span>}
          </span>
        ),
      },
      {
        id: "evaluationDate",
        header: "Respondida em",
        hideOnMobile: true,
        cell: (row) => (
          <span className="text-ink-secondary">{formatLongDate(row.evaluationDate)}</span>
        ),
      },
      {
        id: "actions",
        header: "Ações",
        align: "right",
        width: "w-20",
        cell: (row) => (
          <RowActions>
            <ViewAction onClick={() => setViewing(row)} />
          </RowActions>
        ),
      },
    ],
    [],
  );

  return (
    <>
      <EvaluationsSummary summary={summary} />

      <InfiniteDataTable
        className="min-h-0 flex-1"
        title="Avaliações recebidas"
        columns={columns}
        initialSlice={initialSlice}
        loadPage={loadFilteredPage}
        reloadKey={filterKey}
        getRowId={(row) => row.id}
        describe={(loaded, total) => `${loaded} de ${total} carregadas`}
        toolbar={
          <TableToolbar
            searchPlaceholder="Buscar avaliações"
            filter={
              <EvaluationsFilterModal
                value={filter}
                onApply={setFilter}
                departments={departments}
              />
            }
          />
        }
        emptyMessage={
          filtering
            ? "Nenhuma avaliação encontrada com esses critérios."
            : "Ninguém avaliou uma massagem ainda."
        }
      />

      <ViewEvaluationModal evaluation={viewing} onClose={() => setViewing(null)} />
    </>
  );
}
