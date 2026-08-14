"use client";

import { useState, type FormEvent } from "react";
import { Badge, Button, Icon, Modal, Select } from "@/components/ui";
import type { DepartmentFilter } from "@/features/departments/types/department.types";

export type DepartmentsFilterModalProps = {
  /** Filtros em vigor — a busca por nome não entra aqui. */
  value: DepartmentFilter;
  onApply: (filter: DepartmentFilter) => void;
};

function countActive(filter: DepartmentFilter): number {
  return filter.active === undefined ? 0 : 1;
}

export function DepartmentsFilterModal({ value, onApply }: DepartmentsFilterModalProps) {
  const [open, setOpen] = useState(false);
  const activeCount = countActive(value);

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const active = String(new FormData(event.currentTarget).get("active") ?? "");

    // Campo em branco sai do filtro em vez de virar busca por vazio.
    onApply({ active: active === "" ? undefined : active === "true" });
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
        title="Filtrar departamentos"
        description="Combine com a busca por nome."
        footer={
          <>
            <Button variant="ghost" size="sm" onClick={clear}>
              Limpar
            </Button>
            <Button type="submit" form="departments-filter-form" size="sm">
              Aplicar
            </Button>
          </>
        }
      >
        <form
          // Reabrir precisa mostrar o que está em vigor, não o que foi digitado
          // e abandonado na última vez.
          key={JSON.stringify(value)}
          id="departments-filter-form"
          onSubmit={handleSubmit}
          className="flex flex-col gap-5"
        >
          <Select
            name="active"
            label="Situação"
            defaultValue={value.active === undefined ? "" : String(value.active)}
            options={[
              { label: "Todas", value: "" },
              { label: "Ativos", value: "true" },
              { label: "Inativos", value: "false" },
            ]}
          />
        </form>
      </Modal>
    </>
  );
}
