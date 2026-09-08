"use client";

import { useState, type FormEvent } from "react";
import { Badge, Button, Icon, Input, Modal, Select } from "@/components/ui";
import {
  EVALUATION_SCALE,
  type EvaluationFilter,
  type EvaluationScoreValue,
} from "@/features/evaluations/types/evaluation.types";

export type EvaluationsFilterModalProps = {
  value: EvaluationFilter;
  onApply: (filter: EvaluationFilter) => void;
  /** Departamentos ativos, para recortar o resultado por área. */
  departments: { id: number; name: string }[];
};

function countActive(filter: EvaluationFilter): number {
  return [filter.score, filter.departmentId, filter.from, filter.to].filter(
    (item) => item !== undefined && item !== "",
  ).length;
}

export function EvaluationsFilterModal({
  value,
  onApply,
  departments,
}: EvaluationsFilterModalProps) {
  const [open, setOpen] = useState(false);
  const activeCount = countActive(value);

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const data = new FormData(event.currentTarget);

    const score = String(data.get("score") ?? "");
    const departmentId = String(data.get("departmentId") ?? "");
    const from = String(data.get("from") ?? "");
    const to = String(data.get("to") ?? "");

    // Campo em branco sai do filtro em vez de virar busca por vazio.
    onApply({
      score: score === "" ? undefined : (Number(score) as EvaluationScoreValue),
      departmentId: departmentId === "" ? undefined : Number(departmentId),
      from: from === "" ? undefined : from,
      to: to === "" ? undefined : to,
    });
    setOpen(false);
  };

  const clear = () => {
    onApply({});
    setOpen(false);
  };

  return (
    <>
      <Button
        variant="secondary"
        size="sm"
        onClick={() => setOpen(true)}
        leadingIcon={<Icon name="filter" className="h-4 w-4" />}
      >
        Filtrar
        {activeCount > 0 && (
          <Badge tone="accent" className="ml-1 px-1.5 py-0">
            {activeCount}
          </Badge>
        )}
      </Button>

      <Modal
        open={open}
        onClose={() => setOpen(false)}
        size="sm"
        title="Filtrar avaliações"
        description="O período usa a data em que a nota foi dada."
        footer={
          <>
            <Button variant="ghost" size="sm" onClick={clear}>
              Limpar
            </Button>
            <Button type="submit" form="evaluations-filter-form" size="sm">
              Aplicar
            </Button>
          </>
        }
      >
        <form
          // Reabrir mostra o que está em vigor, não o que foi digitado e
          // abandonado na última vez.
          key={JSON.stringify(value)}
          id="evaluations-filter-form"
          onSubmit={handleSubmit}
          className="flex flex-col gap-5"
        >
          <Select
            name="score"
            label="Nota"
            defaultValue={value.score === undefined ? "" : String(value.score)}
            options={[
              { label: "Todas", value: "" },
              ...[...EVALUATION_SCALE].reverse().map((item) => ({
                label: `${item.emoji} ${item.label}`,
                value: String(item.value),
              })),
            ]}
          />

          <Select
            name="departmentId"
            label="Departamento"
            defaultValue={value.departmentId === undefined ? "" : String(value.departmentId)}
            options={[
              { label: "Todos", value: "" },
              ...departments.map((department) => ({
                label: department.name,
                value: String(department.id),
              })),
            ]}
          />

          <div className="grid grid-cols-2 gap-3">
            <Input name="from" type="date" label="De" defaultValue={value.from ?? ""} />
            <Input name="to" type="date" label="Até" defaultValue={value.to ?? ""} />
          </div>
        </form>
      </Modal>
    </>
  );
}
