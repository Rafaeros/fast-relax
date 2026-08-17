import { Card } from "@/components/ui";
import type { CollaboratorSession } from "@/features/collaborator-portal/types/portal.types";
import { parseApiDate } from "@/features/collaborator-portal/lib/format";

export type MonthlyUsageChartProps = {
  sessions: CollaboratorSession[];
  /** Quantos meses exibir, contando o atual. */
  months?: number;
};

const MONTH_LABELS = ["jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"];

type Bucket = { key: string; label: string; total: number };

/** Meses contínuos até hoje, inclusive os zerados — buraco no meio distorce a leitura. */
function buildBuckets(sessions: CollaboratorSession[], months: number): Bucket[] {
  const buckets: Bucket[] = [];
  const cursor = new Date();
  cursor.setDate(1);

  for (let index = months - 1; index >= 0; index--) {
    const date = new Date(cursor.getFullYear(), cursor.getMonth() - index, 1);
    buckets.push({
      key: `${date.getFullYear()}-${date.getMonth()}`,
      label: MONTH_LABELS[date.getMonth()],
      total: 0,
    });
  }

  const index = new Map(buckets.map((bucket) => [bucket.key, bucket]));

  for (const session of sessions) {
    // Só o que foi de fato usado entra: cancelada e expirada não são consumo.
    if (session.status !== "DONE") continue;

    const date = parseApiDate(session.sessionDate);
    const bucket = index.get(`${date.getFullYear()}-${date.getMonth()}`);
    if (bucket) bucket.total += 1;
  }

  return buckets;
}

/**
 * Sessões concluídas por mês.
 *
 * <p>
 * Série única, então dispensa legenda — o título já diz o que a barra
 * representa. Barras em CSS puro: são poucos pontos e nenhuma interação além
 * do valor já impresso acima de cada uma, o que não justifica uma biblioteca
 * de gráficos no bundle de um app mobile.
 */
export function MonthlyUsageChart({ sessions, months = 6 }: MonthlyUsageChartProps) {
  const buckets = buildBuckets(sessions, months);
  const peak = Math.max(...buckets.map((bucket) => bucket.total), 1);
  const hasData = buckets.some((bucket) => bucket.total > 0);

  return (
    <Card padding="lg" className="flex flex-col gap-4">
      <div className="flex flex-col gap-1">
        <h2 className="text-sm font-semibold text-ink-primary">Massagens concluídas por mês</h2>
        <p className="text-xs text-ink-tertiary">Últimos {months} meses</p>
      </div>

      {hasData ? (
        <div className="flex h-36 items-end gap-2" role="img" aria-label="Gráfico de massagens concluídas por mês">
          {buckets.map((bucket) => (
            <div key={bucket.key} className="flex min-w-0 flex-1 flex-col items-center gap-2">
              <span className="text-xs font-medium tabular-nums text-ink-secondary">
                {bucket.total > 0 ? bucket.total : ""}
              </span>

              {/* O trilho ocupa a altura toda e a barra cresce dentro dele:
                  assim todas as colunas partem da mesma linha de base. */}
              <div className="flex w-full flex-1 items-end">
                <div
                  className="w-full rounded-t bg-accent transition-[height]"
                  style={{
                    // Piso de 4% para o mês zerado não sumir por completo.
                    height: `${Math.max((bucket.total / peak) * 100, bucket.total > 0 ? 8 : 4)}%`,
                    opacity: bucket.total > 0 ? 1 : 0.25,
                  }}
                />
              </div>

              <span className="text-[0.6875rem] text-ink-muted">{bucket.label}</span>
            </div>
          ))}
        </div>
      ) : (
        <p className="py-8 text-center text-sm text-ink-tertiary">
          Ainda não há massagens concluídas para exibir.
        </p>
      )}
    </Card>
  );
}
