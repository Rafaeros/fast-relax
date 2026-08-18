"use client";

import { useCallback, useMemo, useState } from "react";
import {
  Badge,
  DeleteAction,
  EditAction,
  InfiniteDataTable,
  RowActions,
  TableIdentity,
  TableToolbar,
  ViewAction,
  useToast,
} from "@/components/ui";
import type { DataTableColumn } from "@/components/ui";
import type { PageSlice } from "@/lib/api/pagination.types";
import { formatLongDate } from "@/lib/format";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { deleteDepartmentAction } from "@/features/departments/actions/department.actions";
import { CreateDepartmentModal } from "@/features/departments/components/CreateDepartmentModal";
import { DepartmentsFilterModal } from "@/features/departments/components/DepartmentsFilterModal";
import { EditDepartmentModal } from "@/features/departments/components/EditDepartmentModal";
import { ViewDepartmentModal } from "@/features/departments/components/ViewDepartmentModal";
import type {
  Department,
  DepartmentFilter,
} from "@/features/departments/types/department.types";

export type DepartmentsTableProps = {
  initialSlice: PageSlice<Department>;
  loadPage: (page: number, filter: DepartmentFilter) => Promise<PageSlice<Department>>;
};

export function DepartmentsTable({ initialSlice, loadPage }: DepartmentsTableProps) {
  // Incrementar o sinal faz a tabela descartar o que está em tela e recarregar
  // da primeira página — é assim que cadastro e edição aparecem na hora.
  const [reloadSignal, setReloadSignal] = useState(0);
  const [search, setSearch] = useState("");
  const [filters, setFilters] = useState<DepartmentFilter>({});
  // Sem o atraso, cada tecla viraria uma requisição.
  const debouncedSearch = useDebouncedValue(search);
  // Um modal por tela, não um por linha: a lista cresce com a rolagem infinita
  // e montar um diálogo por registro encheria o DOM à toa.
  const [viewing, setViewing] = useState<Department | null>(null);
  const [editing, setEditing] = useState<Department | null>(null);
  const toast = useToast();

  const reload = () => setReloadSignal((current) => current + 1);

  const filter = useMemo<DepartmentFilter>(() => {
    const term = debouncedSearch.trim();
    return term ? { ...filters, name: term } : filters;
  }, [debouncedSearch, filters]);

  // Identidade estável por filtro: sem isso o observer da rolagem infinita
  // seria recriado a cada render.
  const loadFilteredPage = useCallback(
    (page: number) => loadPage(page, filter),
    [loadPage, filter],
  );

  // Filtro novo entra pela mesma porta do "recarregar": volta à página 0.
  const reloadKey = `${reloadSignal}:${JSON.stringify(filter)}`;
  const filtering = Object.keys(filter).length > 0;

  /**
   * As colunas vivem aqui porque `cell` é função — o servidor não consegue
   * serializar isso para um componente cliente — e porque precisam abrir os
   * modais, que são estado desta tabela.
   */
  const columns = useMemo<DataTableColumn<Department>[]>(
    () => [
      {
        id: "name",
        header: "Nome",
        cell: (row) => <TableIdentity name={row.name} />,
      },
      {
        id: "active",
        header: "Situação",
        cell: (row) => (
          <Badge tone={row.active ? "success" : "neutral"}>
            {row.active ? "Ativo" : "Inativo"}
          </Badge>
        ),
      },
      {
        id: "createdAt",
        header: "Cadastrado em",
        hideOnMobile: true,
        cell: (row) => (
          <span className="text-ink-secondary">{formatLongDate(row.createdAt)}</span>
        ),
      },
      {
        id: "actions",
        header: "Ações",
        align: "right",
        width: "w-32",
        cell: (row) => (
          <RowActions>
            <ViewAction onClick={() => setViewing(row)} />
            <EditAction onClick={() => setEditing(row)} />
            <DeleteAction
              itemName={row.name}
              description="Colaboradores vinculados perdem a referência de departamento."
              onConfirm={async () => {
                const result = await deleteDepartmentAction(row.id);
                // A recusa do backend (departamento com colaboradores, por
                // exemplo) passava despercebida: a lista só voltava intacta.
                if (result.ok) {
                  reload();
                  toast.success(result.message);
                } else {
                  toast.error(result.message);
                }
              }}
            />
          </RowActions>
        ),
      },
    ],
    [toast],
  );

  return (
    <>
      <InfiniteDataTable
        className="min-h-0 flex-1"
        title="Lista de departamentos"
        columns={columns}
        initialSlice={initialSlice}
        loadPage={loadFilteredPage}
        reloadKey={reloadKey}
        getRowId={(row) => row.id}
        describe={(loaded, total) => `${loaded} de ${total} carregados`}
        toolbar={
          <TableToolbar
            searchPlaceholder="Buscar por nome"
            searchValue={search}
            onSearchChange={setSearch}
            filter={<DepartmentsFilterModal value={filters} onApply={setFilters} />}
            action={<CreateDepartmentModal onCreated={reload} />}
          />
        }
        emptyMessage={
          filtering
            ? "Nenhum departamento encontrado com esses critérios."
            : "Nenhum departamento cadastrado ainda."
        }
      />

      <ViewDepartmentModal
        department={viewing}
        onClose={() => setViewing(null)}
        onEdit={(department) => {
          setViewing(null);
          setEditing(department);
        }}
      />

      <EditDepartmentModal
        department={editing}
        onClose={() => setEditing(null)}
        onUpdated={reload}
      />
    </>
  );
}
