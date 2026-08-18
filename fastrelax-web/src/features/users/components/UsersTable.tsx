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
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { deleteUserAction } from "@/features/users/actions/user.actions";
import { CreateUserModal } from "@/features/users/components/CreateUserModal";
import { EditUserModal } from "@/features/users/components/EditUserModal";
import { UsersFilterModal } from "@/features/users/components/UsersFilterModal";
import { ViewUserModal } from "@/features/users/components/ViewUserModal";
import type { User, UserFilter } from "@/features/users/types/user.types";

export type UsersTableProps = {
  initialSlice: PageSlice<User>;
  loadPage: (page: number) => Promise<PageSlice<User>>;
};

export function UsersTable({ initialSlice, loadPage }: UsersTableProps) {
  // Incrementar o sinal faz a tabela descartar o que está em tela e recarregar
  // da primeira página — é assim que cadastro e edição aparecem na hora.
  const [reloadSignal, setReloadSignal] = useState(0);
  const [search, setSearch] = useState("");
  const [filters, setFilters] = useState<UserFilter>({});
  const debouncedSearch = useDebouncedValue(search);
  // Um modal por tela, não um por linha: a lista cresce com a rolagem infinita
  // e montar um diálogo por registro encheria o DOM à toa.
  const [viewing, setViewing] = useState<User | null>(null);
  const [editing, setEditing] = useState<User | null>(null);
  const toast = useToast();

  const reload = () => setReloadSignal((current) => current + 1);

  /**
   * `GET /users` aceita só paginação — não há filtro no servidor. A seleção
   * roda sobre as linhas já carregadas; a rolagem continua trazendo páginas,
   * então descer amplia o conjunto pesquisado.
   */
  const filterRow = useCallback(
    (user: User) => {
      const term = debouncedSearch.trim().toLowerCase();

      if (term && !`${user.name} ${user.email}`.toLowerCase().includes(term)) return false;
      if (filters.role && user.role !== filters.role) return false;
      if (filters.active !== undefined && user.active !== filters.active) return false;

      return true;
    },
    [debouncedSearch, filters],
  );

  const filtering =
    debouncedSearch.trim().length > 0 || filters.role !== undefined || filters.active !== undefined;

  /**
   * As colunas vivem aqui porque `cell` é função — o servidor não consegue
   * serializar isso para um componente cliente — e porque precisam abrir os
   * modais, que são estado desta tabela.
   */
  const columns = useMemo<DataTableColumn<User>[]>(
    () => [
      {
        id: "name",
        header: "Nome",
        cell: (row) => <TableIdentity name={row.name} />,
      },
      {
        id: "email",
        header: "E-mail",
        cell: (row) => <span className="text-ink-secondary">{row.email}</span>,
      },
      {
        id: "role",
        header: "Perfil",
        hideOnMobile: true,
        cell: (row) => <Badge tone="neutral">{row.role}</Badge>,
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
              description="O usuário perde o acesso ao painel."
              onConfirm={async () => {
                const result = await deleteUserAction(row.id);
                // A recusa do backend (excluir a si mesmo, por exemplo) passava
                // despercebida: a lista só voltava intacta.
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
        title="Lista de usuários"
        columns={columns}
        initialSlice={initialSlice}
        loadPage={loadPage}
        reloadKey={reloadSignal}
        filterRow={filterRow}
        getRowId={(row) => row.id}
        describe={(loaded, total) =>
          filtering ? `${loaded} de ${total} correspondem` : `${loaded} de ${total} carregados`
        }
        toolbar={
          <TableToolbar
            searchPlaceholder="Buscar por nome ou e-mail"
            searchValue={search}
            onSearchChange={setSearch}
            filter={<UsersFilterModal value={filters} onApply={setFilters} />}
            action={<CreateUserModal onCreated={reload} />}
          />
        }
        emptyMessage={
          filtering
            ? "Nenhum usuário encontrado com esses critérios."
            : "Nenhum usuário cadastrado ainda."
        }
      />

      <ViewUserModal
        user={viewing}
        onClose={() => setViewing(null)}
        onEdit={(user) => {
          setViewing(null);
          setEditing(user);
        }}
        onChanged={reload}
      />

      <EditUserModal user={editing} onClose={() => setEditing(null)} onUpdated={reload} />
    </>
  );
}
