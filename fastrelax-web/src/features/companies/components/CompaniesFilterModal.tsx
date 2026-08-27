"use client";

import { useState, type FormEvent } from "react";
import { Badge, Button, Icon, Modal, Select } from "@/components/ui";
import type { CompanyFilter } from "@/features/companies/types/company.types";

export type CompaniesFilterModalProps = {
  /** Filtros em vigor — a busca por nome/CNPJ não entra aqui. */
  value: CompanyFilter;
  onApply: (filter: CompanyFilter) => void;
};

export function CompaniesFilterModal({ value, onApply }: CompaniesFilterModalProps) {
  const [open, setOpen] = useState(false);
  const activeCount = value.active === undefined ? 0 : 1;

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
        title="Filtrar empresas"
        description="Combine com a busca por razão social ou CNPJ."
        footer={
          <>
            <Button variant="ghost" size="sm" onClick={clear}>
              Limpar
            </Button>
            <Button type="submit" form="companies-filter-form" size="sm">
              Aplicar
            </Button>
          </>
        }
      >
        <form
          // Reabrir precisa mostrar o que está em vigor, não o que foi digitado
          // e abandonado na última vez.
          key={JSON.stringify(value)}
          id="companies-filter-form"
          onSubmit={handleSubmit}
          className="flex flex-col gap-5"
        >
          <Select
            name="active"
            label="Contrato"
            defaultValue={value.active === undefined ? "" : String(value.active)}
            options={[
              { label: "Todos", value: "" },
              { label: "Ativo", value: "true" },
              { label: "Suspenso", value: "false" },
            ]}
          />
        </form>
      </Modal>
    </>
  );
}
