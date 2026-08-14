"use client";

import { useState } from "react";
import { Badge, Button, Icon, IconButton, Input, Select } from "@/components/ui";
import {
  sortByWeekday,
  WORK_DAYS,
  WORK_DAY_LABELS,
  WORK_DAY_SHORT_LABELS,
  type AllowedWindow,
  type WorkDay,
} from "@/features/collaborators/types/schedule.types";

export type AllowedWindowsFieldProps = {
  /** Nome do campo oculto que leva a lista serializada no `FormData`. */
  name?: string;
  disabled?: boolean;
  defaultValue?: AllowedWindow[];
  error?: string;
};

const DEFAULT_START = "12:00";
const DEFAULT_END = "13:00";

/**
 * Editor do horário permitido: um dia da semana com início e fim.
 *
 * A lista vive em estado local e viaja num input oculto em JSON — assim o
 * formulário continua sendo um `<form>` comum, com um `FormData` só, sem
 * precisar de estado compartilhado com o modal.
 */
export function AllowedWindowsField({
  name = "allowedWindows",
  disabled,
  defaultValue = [],
  error,
}: AllowedWindowsFieldProps) {
  const [windows, setWindows] = useState<AllowedWindow[]>(() => sortByWeekday(defaultValue));
  const [start, setStart] = useState(DEFAULT_START);
  const [end, setEnd] = useState(DEFAULT_END);
  const [localError, setLocalError] = useState<string | null>(null);

  // Um dia só pode ter uma janela: a constraint única do banco é por
  // (colaborador, dia), então já removemos os usados da lista de opções.
  const usedDays = new Set(windows.map((window) => window.dayOfWeek));
  const availableDays = WORK_DAYS.filter((day) => !usedDays.has(day));
  const [day, setDay] = useState<WorkDay>(availableDays[0] ?? "MONDAY");

  const add = () => {
    if (!availableDays.includes(day)) {
      setLocalError("Este dia já tem uma janela configurada.");
      return;
    }
    if (end <= start) {
      setLocalError("O término deve ser posterior ao início.");
      return;
    }

    setWindows((current) =>
      sortByWeekday([...current, { dayOfWeek: day, allowedStartTime: start, allowedEndTime: end }]),
    );
    setLocalError(null);

    // Próximo dia livre já selecionado: cadastrar a semana inteira vira
    // uma sequência de cliques, sem voltar ao seletor a cada item.
    const nextDay = availableDays.find((option) => option !== day);
    if (nextDay) setDay(nextDay);
  };

  const remove = (target: WorkDay) => {
    setWindows((current) => current.filter((window) => window.dayOfWeek !== target));
    setLocalError(null);
  };

  const message = localError ?? error;

  return (
    <div className="flex flex-col gap-3">
      <input type="hidden" name={name} value={JSON.stringify(windows)} />

      <div className="flex flex-col gap-1">
        <span className="text-xs font-semibold tracking-wide text-ink-secondary">
          Horário permitido
        </span>
        <span className="text-xs text-ink-tertiary">
          Faixa em que o colaborador pode agendar sessão, por dia da semana.
        </span>
      </div>

      <div className="flex flex-col gap-3 rounded-control border border-line bg-bg-900 p-3">
        <div className="grid gap-3 sm:grid-cols-[1.4fr_1fr_1fr_auto] sm:items-end">
          <Select
            aria-label="Dia da semana"
            value={day}
            onChange={(event) => setDay(event.target.value as WorkDay)}
            disabled={disabled || availableDays.length === 0}
          >
            {availableDays.map((option) => (
              <option key={option} value={option}>
                {WORK_DAY_LABELS[option]}
              </option>
            ))}
          </Select>

          <Input
            type="time"
            aria-label="Início permitido"
            value={start}
            onChange={(event) => setStart(event.target.value)}
            disabled={disabled || availableDays.length === 0}
            className="py-2"
          />

          <Input
            type="time"
            aria-label="Fim permitido"
            value={end}
            onChange={(event) => setEnd(event.target.value)}
            disabled={disabled || availableDays.length === 0}
            className="py-2"
          />

          <Button
            variant="secondary"
            size="sm"
            onClick={add}
            disabled={disabled || availableDays.length === 0}
            leadingIcon={<Icon name="check" className="h-4 w-4" />}
          >
            Adicionar
          </Button>
        </div>

        {windows.length === 0 ? (
          <p className="py-2 text-center text-xs text-ink-tertiary">
            Nenhum dia configurado. Sem horário permitido, o colaborador não consegue agendar.
          </p>
        ) : (
          <ul className="flex flex-col gap-2">
            {windows.map((window) => (
              <li
                key={window.dayOfWeek}
                className="flex items-center gap-3 rounded-control border border-line-soft bg-surface-card px-3 py-2"
              >
                <Badge tone="neutral">{WORK_DAY_SHORT_LABELS[window.dayOfWeek]}</Badge>
                <span className="flex-1 text-sm tabular-nums text-ink-primary">
                  {window.allowedStartTime} às {window.allowedEndTime}
                </span>
                <IconButton
                  label={`Remover ${WORK_DAY_LABELS[window.dayOfWeek]}`}
                  tone="danger"
                  disabled={disabled}
                  onClick={() => remove(window.dayOfWeek)}
                  icon={<Icon name="trash" className="h-4 w-4" />}
                />
              </li>
            ))}
          </ul>
        )}
      </div>

      {message && <p className="text-xs text-error-400">{message}</p>}
    </div>
  );
}
