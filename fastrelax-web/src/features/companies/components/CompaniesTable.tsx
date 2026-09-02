"use client";

import { useCallback, useMemo, useState } from "react";
import {
  Badge,
  DeleteAction,
  EditAction,
  InfiniteDataTable,
  RowAction,
  RowActions,
  TableIdentity,
  TableToolbar,
  ViewAction,
  useToast,
} from "@/components/ui";
import type { DataTableColumn } from "@/components/ui";
import type { PageSlice } from "@/lib/api/pagination.types";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import {
  deleteCompanyAction,
  toggleCompanyActiveAction,
} from "@/features/companies/actions/company.actions";
import { PushCompanyNetworkAction } from "@/features/chairs/components/PushCompanyNetworkAction";
import { CompaniesFilterModal } from "@/features/companies/components/CompaniesFilterModal";
import { CreateCompanyModal } from "@/features/companies/components/CreateCompanyModal";
import { EditCompanyModal } from "@/features/companies/components/EditCompanyModal";
import { ViewCompanyModal } from "@/features/companies/components/ViewCompanyModal";
import type { Company, CompanyFilter } from "@/features/companies/types/company.types";
import type { State } from "@/features/locations/types/location.types";

export type CompaniesTableProps = {
  initialSlice: PageSlice<Company>;
  states: State[];
  loadPage: (page: number) => Promise<PageSlice<Company>>;
};

export function CompaniesTable({ initialSlice, states, loadPage }: CompaniesTableProps) {
  // Incrementar o sinal faz a tabela descartar o que está em tela e recarregar
  // da primeira página — é assim que cadastro e edição aparecem na hora.
  const [reloadSignal, setReloadSignal] = useState(0);
  const [search, setSearch] = useState("");
  const [filters, setFilters] = useState<CompanyFilter>({});
  const debouncedSearch = useDebouncedValue(search);
  // Um modal por tela, não um por linha: a lista cresce com a rolagem infinita
  // e montar um diálogo por registro encheria o DOM à toa.
  const [viewing, setViewing] = useState<Company | null>(null);
  const [editing, setEditing] = useState<Company | null>(null);
  const toast = useToast();

  const reload = () => setReloadSignal((current) => current + 1);

  /**
   * `GET /companies` aceita só paginação — não há filtro no servidor. A seleção
   * roda sobre as linhas já carregadas; a rolagem continua trazendo páginas,
   * então descer amplia o conjunto pesquisado.
   *
   * A busca também casa contra o CNPJ sem máscara: quem digita "12345678" está
   * procurando pelo número, não pela pontuação.
   */
  const filterRow = useCallback(
    (company: Company) => {
      // A pontuação sai dos dois lados: quem digita "12.345" procura o número,
      // e o CNPJ só existe em dígitos na resposta da API.
      const term = strip(debouncedSearch);
      const haystack = strip(`${company.name} ${company.cnpj} ${company.slug} ${company.email}`);

      if (term && !haystack.includes(term)) return false;
      if (filters.active !== undefined && company.active !== filters.active) return false;

      return true;
    },
    [debouncedSearch, filters],
  );

  const filtering = debouncedSearch.trim().length > 0 || filters.active !== undefined;

  /**
   * As colunas vivem aqui porque `cell` é função — o servidor não consegue
   * serializar isso para um componente cliente — e porque precisam abrir os
   * modais, que são estado desta tabela.
   */
  const columns = useMemo<DataTableColumn<Company>[]>(
    () => [
      {
        id: "name",
        header: "Razão social",
        cell: (row) => <TableIdentity name={row.name} secondary={row.slug} />,
      },
      {
        id: "city",
        header: "Cidade",
        hideOnMobile: true,
        cell: (row) => (
          <span className="text-ink-secondary">
            {row.cityName ? `${row.cityName}/${row.stateAcronym ?? ""}` : "—"}
          </span>
        ),
      },
      {
        id: "email",
        header: "Contato",
        hideOnMobile: true,
        cell: (row) => <span className="text-ink-secondary">{row.email}</span>,
      },
      {
        id: "active",
        header: "Contrato",
        cell: (row) => (
          <Badge tone={row.active ? "success" : "neutral"}>
            {row.active ? "Ativo" : "Suspenso"}
          </Badge>
        ),
      },
      {
        id: "actions",
        header: "Ações",
        align: "right",
        width: "w-40",
        cell: (row) => (
          <RowActions>
            <ViewAction onClick={() => setViewing(row)} />
            <EditAction onClick={() => setEditing(row)} />
            <PushCompanyNetworkAction
              companyId={row.id}
              wifiConfigured={row.wifiConfigured}
            />
            {/*
              Suspender é o gesto do dia a dia — contrato em atraso, cliente em
              pausa — e derruba o acesso de todo mundo da empresa de uma vez.
              Remover é definitivo e fica ao lado, com confirmação.
            */}
            <RowAction
              label={row.active ? "Suspender contrato" : "Reativar contrato"}
              icon={row.active ? "lock" : "check"}
              onClick={async () => {
                const result = await toggleCompanyActiveAction(row.id);
                if (result.ok) {
                  reload();
                  toast.success(result.message);
                } else {
                  toast.error(result.message);
                }
              }}
            />
            <DeleteAction
              itemName={row.name}
              description="Colaboradores, cadeiras e sessões da empresa param de aparecer."
              onConfirm={async () => {
                const result = await deleteCompanyAction(row.id);
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
        title="Lista de empresas"
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
            searchPlaceholder="Buscar por razão social, CNPJ ou slug"
            searchValue={search}
            onSearchChange={setSearch}
            filter={<CompaniesFilterModal value={filters} onApply={setFilters} />}
            action={<CreateCompanyModal states={states} onCreated={reload} />}
          />
        }
        emptyMessage={
          filtering
            ? "Nenhuma empresa encontrada com esses critérios."
            : "Nenhuma empresa cadastrada ainda."
        }
      />

      <ViewCompanyModal
        company={viewing}
        onClose={() => setViewing(null)}
        onEdit={(company) => {
          setViewing(null);
          setEditing(company);
        }}
      />

      <EditCompanyModal
        company={editing}
        states={states}
        onClose={() => setEditing(null)}
        onUpdated={reload}
      />
    </>
  );
}

/** Minúsculas sem pontuação, para a busca casar CNPJ digitado com ou sem máscara. */
function strip(value: string): string {
  return value.toLowerCase().replace(/[.\-/\s]/g, "");
}
