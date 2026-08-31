"use client";

import { useState, useTransition } from "react";
import { Button, Card, Icon, Modal, useToast } from "@/components/ui";
import { cn } from "@/lib/cn";
import { bookSessionAction } from "@/features/collaborator-portal/actions/portal.actions";
import { formatTime, isToday, parseApiDate } from "@/features/collaborator-portal/lib/format";
import type { AvailableSlots, SessionSlot } from "@/features/collaborator-portal/types/portal.types";

export type BookingViewProps = {
  collaboratorId: number;
  slots: AvailableSlots;
};

const WEEKDAY_SHORT = ["Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb"];

/**
 * Fluxo de agendamento: escolhe o dia na régua, depois o horário na grade.
 *
 * <p>
 * Os dias vêm prontos da API — só entram os que têm janela configurada e algum
 * horário ainda no futuro. Por isso a régua nunca oferece um dia sem opção.
 */
export function BookingView({ collaboratorId, slots }: BookingViewProps) {
  const [selectedDate, setSelectedDate] = useState(slots.days[0]?.sessionDate ?? "");
  const [selectedSlot, setSelectedSlot] = useState<SessionSlot | null>(null);
  const [selectedChairId, setSelectedChairId] = useState<number | null>(null);
  const [pending, startTransition] = useTransition();
  const { success, error } = useToast();

  const selectedDay = slots.days.find((day) => day.sessionDate === selectedDate);

  const book = () => {
    if (!selectedSlot || !selectedChairId) return;

    startTransition(async () => {
      const result = await bookSessionAction(
        collaboratorId,
        selectedChairId,
        selectedDate,
        selectedSlot.startTime
      );
      // Resposta do servidor vai para o toast: a grade recarrega abaixo e um
      // aviso empurrando os horários mudaria o alvo do toque no meio da ação.
      if (result.ok) {
        success(result.message);
        setSelectedSlot(null);
        setSelectedChairId(null);
      } else {
        error(result.message);
      }
    });
  };

  if (slots.days.length === 0) {
    return (
      <Card padding="lg" className="flex flex-col items-center gap-3 text-center">
        <span className="flex h-12 w-12 items-center justify-center rounded-full bg-surface-hover">
          <Icon name="calendar" className="h-5 w-5 text-ink-muted" />
        </span>
        <div>
          <p className="text-sm font-semibold text-ink-primary">Nenhum horário disponível</p>
          <p className="mt-1 text-xs text-ink-tertiary">
            Não há horários livres na sua janela nos próximos {slots.maxAdvanceDays} dias. Fale com
            o RH se o seu horário permitido estiver errado.
          </p>
        </div>
      </Card>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <Card padding="md" className="flex flex-col gap-3">
        <h2 className="text-sm font-semibold text-ink-primary">Escolha o dia</h2>

        {/*
          Régua rolável: mantém todos os dias acessíveis sem espremer, e o
          snap deixa o gesto no celular parar alinhado em cada cartão.
        */}
        <ul className="-mx-1 flex snap-x snap-mandatory gap-2 overflow-x-auto px-1 pb-1">
          {slots.days.map((day) => {
            const date = parseApiDate(day.sessionDate);
            const active = day.sessionDate === selectedDate;
            const free = day.slots.filter((slot) => slot.availableChairs.length > 0).length;

            return (
              <li key={day.sessionDate} className="snap-start">
                <button
                  type="button"
                  onClick={() => setSelectedDate(day.sessionDate)}
                  aria-pressed={active}
                  className={cn(
                    "flex min-h-[4.5rem] w-16 flex-col items-center justify-center gap-0.5 rounded-control border px-2 py-2",
                    "focus-visible:outline-none focus-visible:shadow-focus",
                    active
                      ? "border-accent bg-accent text-ink-inverse"
                      : "border-line bg-surface-hover/40 text-ink-secondary",
                  )}
                >
                  <span className="text-[0.625rem] uppercase tracking-wide opacity-80">
                    {isToday(day.sessionDate) ? "Hoje" : WEEKDAY_SHORT[date.getDay()]}
                  </span>
                  <span className="text-lg leading-none font-semibold tabular-nums">
                    {date.getDate()}
                  </span>
                  <span className="text-[0.625rem] opacity-80">
                    {free > 0 ? `${free} livre${free > 1 ? "s" : ""}` : "cheio"}
                  </span>
                </button>
              </li>
            );
          })}
        </ul>
      </Card>

      <Card padding="md" className="flex flex-col gap-3">
        <div className="flex items-baseline justify-between gap-2">
          <h2 className="text-sm font-semibold text-ink-primary">Escolha o horário</h2>
          {selectedDay && (
            <span className="text-xs text-ink-tertiary">
              {formatTime(selectedDay.allowedStartTime)} às{" "}
              {formatTime(selectedDay.allowedEndTime)}
            </span>
          )}
        </div>

        {selectedDay && (
          // Grade fluida: três colunas no celular, mais conforme a tela cresce.
          <ul className="grid grid-cols-3 gap-2 sm:grid-cols-4 lg:grid-cols-6">
            {selectedDay.slots.map((slot) => {
              const available = slot.availableChairs.length > 0;
              return (
                <li key={slot.startTime}>
                  <button
                    type="button"
                    disabled={!available || pending}
                    onClick={() => {
                      setSelectedSlot(slot);
                      setSelectedChairId(
                        slot.availableChairs.length === 1 ? slot.availableChairs[0].id : null
                      );
                    }}
                    className={cn(
                      "flex min-h-11 w-full items-center justify-center rounded-control border px-1 text-sm font-medium tabular-nums",
                      "focus-visible:outline-none focus-visible:shadow-focus",
                      available
                        ? "border-accent/40 bg-accent/10 text-ink-primary hover:border-accent hover:bg-accent/20"
                        : // Ocupado permanece visível para o colaborador ver que o
                          // horário existe e já foi tomado.
                          "cursor-not-allowed border-line bg-surface-hover/30 text-ink-muted line-through",
                    )}
                  >
                    {formatTime(slot.startTime)}
                  </button>
                </li>
              );
            })}
          </ul>
        )}

        <p className="text-xs text-ink-tertiary">
          Cada massagem dura {slots.durationMinutes} minutos. Você pode ter uma sessão marcada por
          vez.
        </p>
      </Card>

      {pending && !selectedSlot && (
        <Button size="md" fullWidth disabled leadingIcon={<Icon name="loader" className="h-4 w-4 animate-spin" />}>
          Agendando...
        </Button>
      )}

      {selectedSlot && (
        <Modal
          open={true}
          onClose={() => {
            if (!pending) setSelectedSlot(null);
          }}
          title="Escolha a cadeira"
          description={`Para o horário das ${formatTime(selectedSlot.startTime)}`}
          dismissible={!pending}
          footer={
            <div className="flex w-full gap-3">
              <Button
                variant="ghost"
                fullWidth
                disabled={pending}
                onClick={() => setSelectedSlot(null)}
              >
                Cancelar
              </Button>
              <Button
                variant="primary"
                fullWidth
                disabled={!selectedChairId || pending}
                onClick={book}
                leadingIcon={pending ? <Icon name="loader" className="h-4 w-4 animate-spin" /> : undefined}
              >
                {pending ? "Agendando..." : "Confirmar"}
              </Button>
            </div>
          }
        >
          <div className="flex flex-col gap-2 py-4">
            {selectedSlot.availableChairs.map((chair) => (
              <button
                key={chair.id}
                type="button"
                disabled={pending}
                onClick={() => setSelectedChairId(chair.id)}
                className={cn(
                  "flex items-center justify-between rounded-control border p-4 text-left transition-colors",
                  "focus-visible:outline-none focus-visible:shadow-focus",
                  selectedChairId === chair.id
                    ? "border-accent bg-accent/5 ring-1 ring-accent"
                    : "border-line bg-surface hover:border-accent/50",
                )}
              >
                <span className="font-medium text-ink-primary">{chair.name}</span>
                {selectedChairId === chair.id && (
                  <Icon name="check" className="h-5 w-5 text-accent" />
                )}
              </button>
            ))}
          </div>
        </Modal>
      )}
    </div>
  );
}
