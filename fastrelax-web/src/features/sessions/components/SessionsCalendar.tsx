"use client";

import { Badge, Card, Icon, IconButton, Button } from "@/components/ui";
import { cn } from "@/lib/cn";
import {
  buildCalendarGrid,
  formatMonthLabel,
  WEEKDAY_LABELS,
  type CalendarDay,
} from "@/features/sessions/lib/calendar";
import {
  formatSessionTime,
  SESSION_STATUS_LABELS,
  SESSION_STATUS_TONES,
  type CalendarMonth,
  type CollaboratorSession,
} from "@/features/sessions/types/session.types";

/** Quantas sessões cabem numa célula antes de virar "+N". */
const MAX_VISIBLE_PER_DAY = 3;

export type SessionsCalendarProps = {
  month: CalendarMonth;
  sessions: CollaboratorSession[];
  pending?: boolean;
  /** Dia selecionado, destacado na grade. */
  selectedDate: string | null;
  onSelectDate: (date: string | null) => void;
  onPreviousMonth: () => void;
  onNextMonth: () => void;
  onCurrentMonth: () => void;
};

export function SessionsCalendar({
  month,
  sessions,
  pending,
  selectedDate,
  onSelectDate,
  onPreviousMonth,
  onNextMonth,
  onCurrentMonth,
}: SessionsCalendarProps) {
  const weeks = buildCalendarGrid(month, sessions);

  return (
    <Card padding="lg" className="flex flex-col gap-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <h2 className="font-display text-xl text-ink-primary">{formatMonthLabel(month)}</h2>
          {pending && <Icon name="loader" className="h-4 w-4 animate-spin text-ink-tertiary" />}
        </div>

        <div className="flex items-center gap-2">
          <Button variant="ghost" size="sm" onClick={onCurrentMonth} disabled={pending}>
            Hoje
          </Button>
          <IconButton
            label="Mês anterior"
            onClick={onPreviousMonth}
            disabled={pending}
            icon={<Icon name="chevronLeft" className="h-4 w-4" />}
          />
          <IconButton
            label="Próximo mês"
            onClick={onNextMonth}
            disabled={pending}
            icon={<Icon name="chevronRight" className="h-4 w-4" />}
          />
        </div>
      </div>

      <div className="overflow-x-auto">
        <div className="min-w-3xl">
          <div className="grid grid-cols-7 gap-1.5 pb-2">
            {WEEKDAY_LABELS.map((label) => (
              <span
                key={label}
                className="text-center text-[0.6875rem] font-semibold uppercase tracking-[0.14em] text-ink-muted"
              >
                {label}
              </span>
            ))}
          </div>

          <div className="flex flex-col gap-1.5">
            {weeks.map((week) => (
              <div key={week[0].date} className="grid grid-cols-7 gap-1.5">
                {week.map((day) => (
                  <DayCell
                    key={day.date}
                    day={day}
                    selected={day.date === selectedDate}
                    onSelect={() => onSelectDate(day.date === selectedDate ? null : day.date)}
                  />
                ))}
              </div>
            ))}
          </div>
        </div>
      </div>
    </Card>
  );
}

function DayCell({
  day,
  selected,
  onSelect,
}: {
  day: CalendarDay;
  selected: boolean;
  onSelect: () => void;
}) {
  const visible = day.sessions.slice(0, MAX_VISIBLE_PER_DAY);
  const hidden = day.sessions.length - visible.length;

  return (
    <button
      type="button"
      onClick={onSelect}
      aria-pressed={selected}
      aria-label={`${day.dayOfMonth}: ${day.sessions.length} sessões`}
      className={cn(
        "flex min-h-28 flex-col gap-1.5 rounded-control border p-2 text-left transition-colors",
        "focus-visible:outline-none focus-visible:shadow-focus",
        day.inMonth ? "bg-surface-card" : "bg-bg-900/60",
        selected
          ? "border-accent-strong bg-surface-hover"
          : "border-line hover:border-line-soft hover:bg-surface-hover/50",
      )}
    >
      <span
        className={cn(
          "flex h-6 w-6 items-center justify-center rounded-pill text-xs font-semibold tabular-nums",
          day.isToday && "bg-accent text-ink-inverse",
          !day.isToday && day.inMonth && "text-ink-primary",
          !day.isToday && !day.inMonth && "text-ink-tertiary",
        )}
      >
        {day.dayOfMonth}
      </span>

      <span className="flex flex-col gap-1">
        {visible.map((session) => (
          <span
            key={session.id}
            className="flex items-center gap-1.5 truncate rounded-[4px] bg-bg-900 px-1.5 py-1 text-[0.6875rem] text-ink-secondary"
          >
            <span
              aria-hidden="true"
              className={cn(
                "h-1.5 w-1.5 shrink-0 rounded-full",
                statusDotClass(session.status),
              )}
            />
            <span className="tabular-nums">{formatSessionTime(session.startTime)}</span>
            <span className="truncate">{session.collaboratorName ?? "—"}</span>
          </span>
        ))}

        {hidden > 0 && (
          <span className="px-1.5 text-[0.6875rem] text-ink-tertiary">+{hidden} sessões</span>
        )}
      </span>
    </button>
  );
}

/**
 * A bolinha repete o status que a tabela mostra por extenso — cor sozinha nunca
 * carrega a informação, é atalho visual de quem já leu a legenda.
 */
function statusDotClass(status: CollaboratorSession["status"]): string {
  const tone = SESSION_STATUS_TONES[status];

  return {
    success: "bg-success-400",
    info: "bg-info-500",
    warning: "bg-copper-400",
    error: "bg-error-600",
    neutral: "bg-neutral-400",
  }[tone];
}

/** Legenda dos status, para a bolinha do calendário ter significado. */
export function SessionStatusLegend() {
  return (
    <ul className="flex flex-wrap items-center gap-4">
      {(Object.keys(SESSION_STATUS_LABELS) as (keyof typeof SESSION_STATUS_LABELS)[]).map(
        (status) => (
          <li key={status} className="flex items-center gap-2">
            <span aria-hidden="true" className={cn("h-2 w-2 rounded-full", statusDotClass(status))} />
            <Badge tone={SESSION_STATUS_TONES[status]}>{SESSION_STATUS_LABELS[status]}</Badge>
          </li>
        ),
      )}
    </ul>
  );
}
