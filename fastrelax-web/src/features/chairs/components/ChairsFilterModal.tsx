"use client";

import { useState, type FormEvent } from "react";
import { Badge, Button, Icon, Modal, Select } from "@/components/ui";
import type { ChairFilter } from "@/features/chairs/types/chair.types";

export type ChairsFilterModalProps = {
  /** Filtros em vigor — a busca por nome não entra aqui. */
  value: ChairFilter;
  onApply: (filter: ChairFilter) => void;
};

function countActive(filter: ChairFilter): number {
  return [filter.active, filter.online].filter((entry) => entry !== undefined).length;
}

/** Campo em branco sai do filtro em vez de virar busca por vazio. */
function toBoolean(value: string): boolean | undefined {
  return value === "" ? undefined : value === "true";
}

export function ChairsFilterModal({ value, onApply }: ChairsFilterModalProps) {
  const [open, setOpen] = useState(false);
  const activeCount = countActive(value);

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);

    onApply({
      active: toBoolean(String(formData.get("active") ?? "")),
      online: toBoolean(String(formData.get("online") ?? "")),
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
        title="Filtrar cadeiras"
        description="Combine com a busca por nome."
        footer={
          <>
            <Button variant="ghost" size="sm" onClick={clear}>
              Limpar
            </Button>
            <Button type="submit" form="chairs-filter-form" size="sm">
              Aplicar
            </Button>
          </>
        }
      >
        <form
          // Reabrir precisa mostrar o que está em vigor, não o que foi digitado
          // e abandonado na última vez.
          key={JSON.stringify(value)}
          id="chairs-filter-form"
          onSubmit={handleSubmit}
          className="flex flex-col gap-5"
        >
          <Select
            name="active"
            label="Situação"
            defaultValue={value.active === undefined ? "" : String(value.active)}
            options={[
              { label: "Todas", value: "" },
              { label: "Ativas", value: "true" },
              { label: "Inativas", value: "false" },
            ]}
          />

          <Select
            name="online"
            label="Conexão"
            hint="Baseada no último sinal recebido do ESP32."
            defaultValue={value.online === undefined ? "" : String(value.online)}
            options={[
              { label: "Todas", value: "" },
              { label: "Online", value: "true" },
              { label: "Offline", value: "false" },
            ]}
          />
        </form>
      </Modal>
    </>
  );
}
