import type {
  CalendarMonth,
  CollaboratorSession,
} from "@/features/sessions/types/session.types";

export type CalendarDay = {
  /** "YYYY-MM-DD" — mesma chave usada em `sessionDate`. */
  date: string;
  dayOfMonth: number;
  /** Falso para os dias de preenchimento vindos do mês vizinho. */
  inMonth: boolean;
  isToday: boolean;
  sessions: CollaboratorSession[];
};

export const WEEKDAY_LABELS = ["Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb"];

const monthFormatter = new Intl.DateTimeFormat("pt-BR", { month: "long", year: "numeric" });

/** "outubro de 2026" com a inicial maiúscula. */
export function formatMonthLabel({ year, month }: CalendarMonth): string {
  const label = monthFormatter.format(new Date(year, month - 1, 1));
  return label.charAt(0).toUpperCase() + label.slice(1);
}

/** Datas do mês em ISO, para o filtro `from`/`to` da API. */
export function monthRange({ year, month }: CalendarMonth): { from: string; to: string } {
  // Dia 0 do mês seguinte é o último dia deste — evita tabela de 28/30/31.
  const lastDay = new Date(year, month, 0).getDate();

  return {
    from: toIsoDate(year, month, 1),
    to: toIsoDate(year, month, lastDay),
  };
}

export function shiftMonth({ year, month }: CalendarMonth, offset: number): CalendarMonth {
  const reference = new Date(year, month - 1 + offset, 1);
  return { year: reference.getFullYear(), month: reference.getMonth() + 1 };
}

export function currentMonth(): CalendarMonth {
  const now = new Date();
  return { year: now.getFullYear(), month: now.getMonth() + 1 };
}

/**
 * Grade do mês em semanas de domingo a sábado, com os dias vizinhos que
 * completam a primeira e a última linha — sem eles as colunas desalinhariam
 * do cabeçalho de dias da semana.
 */
export function buildCalendarGrid(
  { year, month }: CalendarMonth,
  sessions: CollaboratorSession[],
): CalendarDay[][] {
  const byDate = new Map<string, CollaboratorSession[]>();
  for (const session of sessions) {
    const list = byDate.get(session.sessionDate);
    if (list) list.push(session);
    else byDate.set(session.sessionDate, [session]);
  }

  for (const list of byDate.values()) {
    list.sort((a, b) => a.startTime.localeCompare(b.startTime));
  }

  const first = new Date(year, month - 1, 1);
  const start = new Date(first);
  start.setDate(first.getDate() - first.getDay());

  const todayIso = toIsoDateFrom(new Date());
  const weeks: CalendarDay[][] = [];
  const cursor = new Date(start);

  // Seis semanas cobrem qualquer mês; a última é descartada quando sobra vazia.
  for (let week = 0; week < 6; week += 1) {
    const days: CalendarDay[] = [];

    for (let weekday = 0; weekday < 7; weekday += 1) {
      const date = toIsoDateFrom(cursor);

      days.push({
        date,
        dayOfMonth: cursor.getDate(),
        inMonth: cursor.getMonth() === month - 1,
        isToday: date === todayIso,
        sessions: byDate.get(date) ?? [],
      });

      cursor.setDate(cursor.getDate() + 1);
    }

    weeks.push(days);
  }

  return weeks.filter((week, index) => index < 5 || week.some((day) => day.inMonth));
}

function toIsoDate(year: number, month: number, day: number): string {
  return `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
}

function toIsoDateFrom(date: Date): string {
  return toIsoDate(date.getFullYear(), date.getMonth() + 1, date.getDate());
}
