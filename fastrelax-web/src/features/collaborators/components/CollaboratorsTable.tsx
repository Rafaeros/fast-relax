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
} from "@/components/ui";
import type { DataTableColumn } from "@/components/ui";
import type { PageSlice } from "@/lib/api/pagination.types";
import { formatCpf, formatPhone, onlyDigits } from "@/lib/format";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { deleteCollaboratorAction } from "@/features/collaborators/actions/collaborator.actions";
import { CollaboratorsFilterModal } from "@/features/collaborators/components/CollaboratorsFilterModal";
import { CreateCollaboratorModal } from "@/features/collaborators/components/CreateCollaboratorModal";
import { ExportCollaboratorsButton } from "@/features/collaborators/components/ExportCollaboratorsButton";
import { ImportCollaboratorsModal } from "@/features/collaborators/components/ImportCollaboratorsModal";
import { EditCollaboratorModal } from "@/features/collaborators/components/EditCollaboratorModal";
import { ViewCollaboratorModal } from "@/features/collaborators/components/ViewCollaboratorModal";
import type {
  Collaborator,
  CollaboratorFilter,
} from "@/features/collaborators/types/collaborator.types";
import type { Department } from "@/features/departments/types/department.types";

export type CollaboratorsTableProps = {
  initialSlice: PageSlice<Collaborator>;
  loadPage: (page: number, filter: CollaboratorFilter) => Promise<PageSlice<Collaborator>>;
  departments: Department[];
};

export function CollaboratorsTable({
  initialSlice,
  loadPage,
  departments,
}: CollaboratorsTableProps) {
  // Incrementar o sinal faz a tabela descartar o que está em tela e recarregar
  // da primeira página — é assim que cadastro e edição aparecem na hora.
  const [reloadSignal, setReloadSignal] = useState(0);
  const [search, setSearch] = useState("");
  const [filters, setFilters] = useState<CollaboratorFilter>({});
  // Sem o atraso, cada tecla viraria uma requisição.
  const debouncedSearch = useDebouncedValue(search);
  // Um modal por tela, não um por linha: a lista cresce com a rolagem infinita
  // e montar um diálogo por registro encheria o DOM à toa.
  const [viewing, setViewing] = useState<Collaborator | null>(null);
  const [editing, setEditing] = useState<Collaborator | null>(null);

  const reload = () => setReloadSignal((current) => current + 1);

  /**
   * O backend tem dois filtros distintos: `name` casa por trecho, `cpf` casa
   * exato pelo blind index e exige os 11 dígitos. Um campo de busca só, então,
   * decide pelo formato do que foi digitado.
   */
  const filter = useMemo<CollaboratorFilter>(() => {
    const term = debouncedSearch.trim();
    const digits = onlyDigits(term);

    if (digits.length === 11) return { ...filters, cpf: digits };
    if (term) return { ...filters, name: term };
    return filters;
  }, [debouncedSearch, filters]);

  // Identidade estável por filtro: sem isso o observer da rolagem infinita
  // seria recriado a cada render.
  const loadFilteredPage = useCallback(
    (page: number) => loadPage(page, filter),
    [loadPage, filter],
  );

  // Filtro novo entra pela mesma porta do "recarregar": a tabela volta à página 0.
  const reloadKey = `${reloadSignal}:${JSON.stringify(filter)}`;
  const filtering = Object.keys(filter).length > 0;

  /**
   * As colunas vivem aqui porque `cell` é função — o servidor não consegue
   * serializar isso para um componente cliente — e porque precisam abrir os
   * modais, que são estado desta tabela.
   */
  const columns = useMemo<DataTableColumn<Collaborator>[]>(
    () => [
      {
        id: "name",
        header: "Nome",
        cell: (row) => (
          <TableIdentity name={row.name} secondary={row.departmentName ?? undefined} />
        ),
      },
      {
        id: "cpf",
        header: "CPF",
        cell: (row) => <span className="tabular-nums">{formatCpf(row.cpf)}</span>,
      },
      {
        id: "phone",
        header: "Telefone",
        hideOnMobile: true,
        cell: (row) => <span className="tabular-nums">{formatPhone(row.phoneNumber)}</span>,
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
              description="O colaborador deixa de agendar sessões e sai das listagens."
              onConfirm={async () => {
                await deleteCollaboratorAction(row.id);
                reload();
              }}
            />
          </RowActions>
        ),
      },
    ],
    [],
  );

  return (
    <>
      <InfiniteDataTable
        className="min-h-0 flex-1"
        title="Lista de colaboradores"
        columns={columns}
        initialSlice={initialSlice}
        loadPage={loadFilteredPage}
        reloadKey={reloadKey}
        getRowId={(row) => row.id}
        describe={(loaded, total) => `${loaded} de ${total} carregados`}
        toolbar={
          <TableToolbar
            searchPlaceholder="Buscar por nome ou CPF"
            searchValue={search}
            onSearchChange={setSearch}
            filter={
              <CollaboratorsFilterModal
                departments={departments}
                value={filters}
                onApply={setFilters}
              />
            }
            action={
              <CreateCollaboratorModal departments={departments} onCreated={reload} />
            }
          >
            <ImportCollaboratorsModal onImported={reload} />
            <ExportCollaboratorsButton />
          </TableToolbar>
        }
        emptyMessage={
          filtering
            ? "Nenhum colaborador encontrado com esses critérios."
            : "Nenhum colaborador cadastrado ainda."
        }
      />

      <ViewCollaboratorModal
        collaborator={viewing}
        onClose={() => setViewing(null)}
        onEdit={(collaborator) => {
          setViewing(null);
          setEditing(collaborator);
        }}
      />

      <EditCollaboratorModal
        collaborator={editing}
        departments={departments}
        onClose={() => setEditing(null)}
        onUpdated={reload}
      />
    </>
  );
}
