"use client";

import { useState, type FormEvent } from "react";
import { Badge, Button, Icon, MaskedInput, Modal, Select } from "@/components/ui";
import { onlyDigits } from "@/lib/format";
import type { CollaboratorFilter } from "@/features/collaborators/types/collaborator.types";
import type { Department } from "@/features/departments/types/department.types";

export type CollaboratorsFilterModalProps = {
  departments: Department[];
  /** Filtros em vigor — a busca por nome/CPF não entra aqui. */
  value: CollaboratorFilter;
  onApply: (filter: CollaboratorFilter) => void;
};

/** Quantos filtros estão em vigor — vira contador no botão. */
function countActive(filter: CollaboratorFilter): number {
  return [filter.departmentId, filter.active, filter.phoneNumber].filter(
    (entry) => entry !== undefined && entry !== "",
  ).length;
}

export function CollaboratorsFilterModal({
  departments,
  value,
  onApply,
}: CollaboratorsFilterModalProps) {
  const [open, setOpen] = useState(false);
  const activeCount = countActive(value);

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);

    const departmentId = String(formData.get("departmentId") ?? "");
    const active = String(formData.get("active") ?? "");
    const phoneNumber = onlyDigits(String(formData.get("phoneNumber") ?? ""));

    onApply({
      // Campo em branco sai do filtro em vez de virar busca por vazio.
      departmentId: departmentId ? Number(departmentId) : undefined,
      active: active === "" ? undefined : active === "true",
      phoneNumber: phoneNumber || undefined,
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
        title="Filtrar colaboradores"
        description="Combine com a busca por nome ou CPF."
        footer={
          <>
            <Button variant="ghost" size="sm" onClick={clear}>
              Limpar
            </Button>
            <Button type="submit" form="collaborators-filter-form" size="sm">
              Aplicar
            </Button>
          </>
        }
      >
        <form
          // Reabrir precisa mostrar o que está em vigor, não o que foi digitado
          // e abandonado na última vez.
          key={JSON.stringify(value)}
          id="collaborators-filter-form"
          onSubmit={handleSubmit}
          className="flex flex-col gap-5"
        >
          <Select
            name="departmentId"
            label="Departamento"
            defaultValue={value.departmentId ?? ""}
          >
            <option value="">Todos os departamentos</option>
            {departments.map((department) => (
              <option key={department.id} value={department.id}>
                {department.name}
              </option>
            ))}
          </Select>

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

          <MaskedInput
            mask="phone"
            name="phoneNumber"
            label="Telefone"
            placeholder="(11) 90000-0000"
            hint="Busca por parte do número."
            defaultValue={value.phoneNumber ?? ""}
            leadingIcon={<Icon name="phone" />}
          />
        </form>
      </Modal>
    </>
  );
}
