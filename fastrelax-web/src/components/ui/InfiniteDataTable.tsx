"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { Button } from "@/components/ui/Button";
import { DataTable, type DataTableProps } from "@/components/ui/DataTable";
import { Icon } from "@/components/ui/Icon";
import type { PageSlice } from "@/lib/api/pagination.types";

export type InfiniteDataTableProps<TRow> = Omit<
  DataTableProps<TRow>,
  "rows" | "afterRows" | "description"
> & {
  /** Primeira página, buscada no servidor — a tabela já nasce preenchida. */
  initialSlice: PageSlice<TRow>;
  /** Busca a próxima página. Recebe o índice (0-based) e roda como Server Action. */
  loadPage: (page: number) => Promise<PageSlice<TRow>>;
  /** Texto do cabeçalho; recebe quantos itens já apareceram e o total. */
  describe?: (loaded: number, total: number) => string;
  /**
   * Trocar esta chave descarta o que está em tela e recarrega da primeira
   * página. Aceita string para embutir o filtro atual — assim mudar o filtro
   * e criar/editar/excluir um registro usam o mesmo mecanismo.
   */
  reloadKey?: string | number;
  /**
   * Filtro aplicado às linhas já carregadas, para recursos cujo endpoint não
   * aceita filtro no servidor. A rolagem continua trazendo páginas, então o
   * conjunto cresce conforme o usuário desce.
   */
  filterRow?: (row: TRow) => boolean;
};

/**
 * Tabela com rolagem infinita.
 *
 * O sentinela fica dentro da própria área de rolagem do `DataTable` (marcada
 * com `data-scroll-root`), então a próxima página entra quando o usuário chega
 * ao fim da **tabela** — não ao fim da janela.
 */
export function InfiniteDataTable<TRow>({
  initialSlice,
  loadPage,
  describe,
  reloadKey = 0,
  filterRow,
  ...tableProps
}: InfiniteDataTableProps<TRow>) {
  const [rows, setRows] = useState<TRow[]>(initialSlice.rows);
  const [page, setPage] = useState(initialSlice.page);
  const [hasMore, setHasMore] = useState(initialSlice.hasMore);
  const [total, setTotal] = useState(initialSlice.totalElements);
  const [failed, setFailed] = useState(false);
  const [appliedKey, setAppliedKey] = useState(reloadKey);

  // Ajuste de estado durante o render (padrão do React para estado derivado de
  // prop): zera a lista e volta para "antes da página 0". O sentinela fica
  // visível na tabela vazia e o observer busca a primeira página em seguida.
  if (reloadKey !== appliedKey) {
    setAppliedKey(reloadKey);
    setRows([]);
    setPage(-1);
    setHasMore(true);
    setFailed(false);
  }

  const sentinelRef = useRef<HTMLDivElement>(null);
  // Trava síncrona: o observer pode disparar duas vezes antes do estado atualizar.
  const loadingRef = useRef(false);
  const [loading, setLoading] = useState(false);

  const loadNext = useCallback(async () => {
    if (loadingRef.current || !hasMore) return;

    loadingRef.current = true;
    setLoading(true);
    setFailed(false);

    try {
      const slice = await loadPage(page + 1);

      // Página vazia sem `hasMore` também é o sinal de fim vindo do servidor.
      setRows((current) => [...current, ...slice.rows]);
      setPage(slice.page);
      setHasMore(slice.hasMore);
      // O total vem de cada resposta: com filtro aplicado ele muda.
      setTotal(slice.totalElements);
    } catch {
      // Falha de rede: para de pedir e oferece nova tentativa manual.
      setFailed(true);
      setHasMore(false);
    } finally {
      loadingRef.current = false;
      setLoading(false);
    }
  }, [hasMore, loadPage, page]);

  useEffect(() => {
    const sentinel = sentinelRef.current;
    if (!sentinel || !hasMore) return;

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((entry) => entry.isIntersecting)) {
          void loadNext();
        }
      },
      {
        // Sem root explícito o observer usaria a janela, que aqui nunca rola.
        root: sentinel.closest("[data-scroll-root]"),
        // Antecipa a busca antes de o usuário bater no fim.
        rootMargin: "240px",
      },
    );

    observer.observe(sentinel);
    return () => observer.disconnect();
  }, [hasMore, loadNext]);

  const retry = () => {
    setHasMore(true);
    setFailed(false);
    void loadNext();
  };

  const visibleRows = filterRow ? rows.filter(filterRow) : rows;

  return (
    <DataTable
      {...tableProps}
      rows={visibleRows}
      description={describe?.(visibleRows.length, total)}
      afterRows={
        <div ref={sentinelRef} className="flex items-center justify-center px-5 py-4">
          {loading && (
            <span className="flex items-center gap-2 text-xs text-ink-tertiary">
              <Icon name="loader" className="h-4 w-4 animate-spin" />
              Carregando mais registros...
            </span>
          )}

          {failed && (
            <div className="flex items-center gap-3 text-xs text-error-400">
              <span>Não foi possível carregar mais registros.</span>
              <Button variant="secondary" size="sm" onClick={retry}>
                Tentar novamente
              </Button>
            </div>
          )}

          {!loading && !failed && !hasMore && rows.length > 0 && (
            <span className="text-xs text-ink-tertiary">Fim da lista</span>
          )}
        </div>
      }
    />
  );
}
