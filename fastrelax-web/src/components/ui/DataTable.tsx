import type { ReactNode } from "react";
import { cn } from "@/lib/cn";
import { Card } from "@/components/ui/Card";

export type ColumnAlign = "left" | "center" | "right";

export type DataTableColumn<TRow> = {
  /** Identificador único da coluna (usado como key do React). */
  id: string;
  header: ReactNode;
  /** Conteúdo da célula — recebe a linha inteira. */
  cell: (row: TRow) => ReactNode;
  align?: ColumnAlign;
  /** Largura fixa opcional, ex.: "w-32". */
  width?: string;
  /** Oculta a coluna abaixo de `sm` — para campos secundários no mobile. */
  hideOnMobile?: boolean;
};

export type DataTableProps<TRow> = {
  columns: DataTableColumn<TRow>[];
  rows: TRow[];
  getRowId: (row: TRow) => string | number;
  /** Título da tabela, exibido no cabeçalho do card. */
  title?: string;
  /** Texto de apoio ao lado do título (ex.: total de registros). */
  description?: string;
  /** Ações no topo (busca, botão "novo", filtros). */
  toolbar?: ReactNode;
  /** Rodapé (paginação). */
  footer?: ReactNode;
  /**
   * Conteúdo logo abaixo das linhas, **dentro** da área de rolagem — é onde a
   * rolagem infinita coloca o sentinela e o indicador de carregamento.
   */
  afterRows?: ReactNode;
  emptyMessage?: string;
  className?: string;
};

const ALIGN: Record<ColumnAlign, string> = {
  left: "text-left",
  center: "text-center",
  right: "text-right",
};

/**
 * Tabela genérica do design system.
 *
 * Só sabe renderizar colunas — quem chama define o formato de cada célula.
 * Isso permite usar a mesma tabela para colaboradores, usuários, sessões etc.
 * sem variantes específicas por tela.
 */
export function DataTable<TRow>({
  columns,
  rows,
  getRowId,
  title,
  description,
  toolbar,
  footer,
  afterRows,
  emptyMessage = "Nenhum registro encontrado.",
  className,
}: DataTableProps<TRow>) {
  const hasHeader = Boolean(title || description || toolbar);

  return (
    <Card padding="none" className={cn("flex min-h-0 flex-col", className)}>
      {hasHeader && (
        <div className="flex shrink-0 flex-col gap-3 border-b border-line p-5 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex flex-col gap-1">
            {title && <h2 className="text-base font-semibold text-ink-primary">{title}</h2>}
            {description && <p className="text-xs text-ink-tertiary">{description}</p>}
          </div>
          {toolbar && <div className="flex items-center gap-2">{toolbar}</div>}
        </div>
      )}

      {/*
        Rolagem própria nos dois eixos: tabela larga não empurra a página e a
        lista longa desce aqui dentro, sem esticar o layout além de uma tela.
      */}
      {/*
        `flex-[1_1_auto]` em vez de `flex-1`: com base 0, a área rolável
        colapsaria para altura zero quando o card não tem altura definida —
        e a tabela apareceria vazia. Com base automática ela cresce com o
        conteúdo e continua encolhendo (e rolando) quando o pai limita a altura.
      */}
      <div data-scroll-root="" className="min-h-0 w-full flex-[1_1_auto] overflow-auto">
        <table className="w-full min-w-max border-collapse text-sm">
          <thead>
            <tr>
              {columns.map((column) => (
                <th
                  key={column.id}
                  scope="col"
                  className={cn(
                    "whitespace-nowrap px-5 py-3",
                    "text-[0.6875rem] font-semibold uppercase tracking-[0.14em] text-ink-muted",
                    // Fica visível durante a rolagem vertical. A borda vai como
                    // inset shadow: borda comum some em elemento sticky.
                    "sticky top-0 z-10 bg-surface-card",
                    "shadow-[inset_0_-1px_0_var(--color-line)]",
                    ALIGN[column.align ?? "left"],
                    column.width,
                    column.hideOnMobile && "hidden sm:table-cell",
                  )}
                >
                  {column.header}
                </th>
              ))}
            </tr>
          </thead>

          <tbody>
            {rows.length === 0 ? (
              <tr>
                <td
                  colSpan={columns.length}
                  className="px-5 py-12 text-center text-sm text-ink-tertiary"
                >
                  {emptyMessage}
                </td>
              </tr>
            ) : (
              rows.map((row) => (
                <tr
                  key={getRowId(row)}
                  className="border-b border-line-soft/60 transition-colors last:border-b-0 hover:bg-surface-hover/60"
                >
                  {columns.map((column) => (
                    <td
                      key={column.id}
                      className={cn(
                        "px-5 py-3.5 text-ink-secondary",
                        ALIGN[column.align ?? "left"],
                        column.hideOnMobile && "hidden sm:table-cell",
                      )}
                    >
                      {column.cell(row)}
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>

        {afterRows}
      </div>

      {footer && <div className="shrink-0 border-t border-line p-4">{footer}</div>}
    </Card>
  );
}

/** Célula de identificação: avatar com inicial + nome e texto secundário. */
export function TableIdentity({
  name,
  secondary,
}: {
  name: string;
  secondary?: string;
}) {
  return (
    <span className="flex items-center gap-3">
      <span className="grid h-9 w-9 shrink-0 place-items-center rounded-pill border border-line bg-bg-900 text-xs font-semibold text-accent-soft">
        {name.charAt(0).toUpperCase()}
      </span>
      <span className="flex flex-col">
        <span className="font-medium text-ink-primary">{name}</span>
        {secondary && <span className="text-xs text-ink-tertiary">{secondary}</span>}
      </span>
    </span>
  );
}
