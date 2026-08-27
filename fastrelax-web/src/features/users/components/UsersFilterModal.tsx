"use client";

import { useState, type FormEvent } from "react";
import { Badge, Button, Icon, Modal, Select } from "@/components/ui";
import type { UserFilter, UserRole } from "@/features/users/types/user.types";

export type UsersFilterModalProps = {
  /** Filtros em vigor — a busca por nome/e-mail não entra aqui. */
  value: UserFilter;
  onApply: (filter: UserFilter) => void;
};

function countActive(filter: UserFilter): number {
  return [filter.role, filter.active].filter((entry) => entry !== undefined).length;
}

export function UsersFilterModal({ value, onApply }: UsersFilterModalProps) {
  const [open, setOpen] = useState(false);
  const activeCount = countActive(value);

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const role = String(formData.get("role") ?? "");
    const active = String(formData.get("active") ?? "");

    // Campo em branco sai do filtro em vez de virar busca por vazio.
    onApply({
      role: role ? (role as UserRole) : undefined,
      active: active === "" ? undefined : active === "true",
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
        title="Filtrar usuários"
        description="Aplicado sobre os usuários já carregados na lista."
        footer={
          <>
            <Button variant="ghost" size="sm" onClick={clear}>
              Limpar
            </Button>
            <Button type="submit" form="users-filter-form" size="sm">
              Aplicar
            </Button>
          </>
        }
      >
        <form
          // Reabrir precisa mostrar o que está em vigor, não o que foi digitado
          // e abandonado na última vez.
          key={JSON.stringify(value)}
          id="users-filter-form"
          onSubmit={handleSubmit}
          className="flex flex-col gap-5"
        >
          <Select
            name="role"
            label="Perfil"
            defaultValue={value.role ?? ""}
            options={[
              { label: "Todos", value: "" },
              { label: "Administrador da plataforma", value: "SYSADMIN" },
              { label: "Gestor da empresa", value: "COMPANY_ADMIN" },
              { label: "RH da empresa", value: "COMPANY_RH" },
            ]}
          />

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
