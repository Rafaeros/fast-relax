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
import { formatLongDate } from "@/lib/format";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import {
  deleteChairAction,
  toggleChairActiveAction,
} from "@/features/chairs/actions/chair.actions";
import { ChairsFilterModal } from "@/features/chairs/components/ChairsFilterModal";
import { CreateChairModal } from "@/features/chairs/components/CreateChairModal";
import { EditChairModal } from "@/features/chairs/components/EditChairModal";
import { ViewChairModal } from "@/features/chairs/components/ViewChairModal";
import { PushNetworkAction } from "@/features/chairs/components/PushNetworkAction";
import type { Chair, ChairFilter, FirmwareOption } from "@/features/chairs/types/chair.types";

export type ChairsTableProps = {
  initialSlice: PageSlice<Chair>;
  loadPage: (page: number, filter: ChairFilter) => Promise<PageSlice<Chair>>;
  /**
   * Resolvido no servidor e repassado até o modal de detalhes: o teste de relé
   * é restrito a ADMIN. Esconder aqui é conveniência — quem barra de fato é o
   * `@PreAuthorize` do backend.
   */
  isAdmin?: boolean;
  /**
   * Quem está logado é da equipe da plataforma.
   *
   * Muda a tabela em duas frentes: aparece a coluna da empresa — a listagem
   * atravessa clientes — e a ação de gravar a rede no ESP32, que é dela.
   */
  isPlatformTeam?: boolean;
  /** Versões do catálogo, para registrar o firmware gravado em cada cadeira. */
  firmwares?: FirmwareOption[];
};

export function ChairsTable({
  initialSlice,
  loadPage,
  isAdmin = false,
  isPlatformTeam = false,
  firmwares = [],
}: ChairsTableProps) {
  // Incrementar o sinal faz a tabela descartar o que está em tela e recarregar
  // da primeira página — é assim que cadastro e edição aparecem na hora.
  const [reloadSignal, setReloadSignal] = useState(0);
  const [search, setSearch] = useState("");
  const [filters, setFilters] = useState<ChairFilter>({});
  // Sem o atraso, cada tecla viraria uma requisição.
  const debouncedSearch = useDebouncedValue(search);
  // Um modal por tela, não um por linha: a lista cresce com a rolagem infinita
  // e montar um diálogo por registro encheria o DOM à toa.
  const [viewing, setViewing] = useState<Chair | null>(null);
  const [editing, setEditing] = useState<Chair | null>(null);
  const toast = useToast();

  const reload = () => setReloadSignal((current) => current + 1);

  const filter = useMemo<ChairFilter>(() => {
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
  const columns = useMemo<DataTableColumn<Chair>[]>(
    () => [
      {
        id: "name",
        header: "Nome",
        cell: (row) => <TableIdentity name={row.name} secondary={row.macAddress} />,
      },
      // A coluna só existe para a equipe da plataforma: a listagem dela
      // atravessa clientes, e sem o nome o parque vira uma lista sem contexto.
      // Para quem opera dentro de uma empresa é sempre a própria.
      ...(isPlatformTeam
        ? [
            {
              id: "company",
              header: "Empresa",
              cell: (row: Chair) => (
                <span className="text-ink-secondary">{row.companyName ?? "—"}</span>
              ),
            } as DataTableColumn<Chair>,
          ]
        : []),
      {
        id: "online",
        header: "Conexão",
        cell: (row) => (
          <Badge tone={row.online ? "success" : "warning"}>
            {row.online ? "Online" : "Offline"}
          </Badge>
        ),
      },
      // Estado do provisionamento, e não da conexão: uma cadeira pode estar
      // online pela rede antiga e ainda assim fora da configuração atual —
      // é essa a que some quando o AP velho for desligado.
      ...(isPlatformTeam
        ? [
            {
              id: "network",
              header: "Rede",
              hideOnMobile: true,
              cell: (row: Chair) => (
                <Badge tone={networkTone(row)}>{networkLabel(row)}</Badge>
              ),
            } as DataTableColumn<Chair>,
          ]
        : []),
      {
        id: "address",
        header: "Endereço",
        hideOnMobile: true,
        cell: (row) => (
          <span className="text-ink-secondary">
            {row.ipAddress ? `${row.ipAddress}:${row.port}` : "Aguardando sinal"}
          </span>
        ),
      },
      {
        id: "active",
        header: "Situação",
        cell: (row) => (
          <Badge tone={row.active ? "success" : "neutral"}>
            {row.active ? "Ativa" : "Inativa"}
          </Badge>
        ),
      },
      {
        id: "lastSeenAt",
        header: "Último sinal",
        hideOnMobile: true,
        cell: (row) => (
          <span className="text-ink-secondary">{formatLongDate(row.lastSeenAt)}</span>
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
            {isPlatformTeam && <PushNetworkAction chair={row} onPushed={reload} />}
            <RowAction
              label={row.active ? "Desativar" : "Ativar"}
              icon={row.active ? "eyeOff" : "check"}
              onClick={async () => {
                const result = await toggleChairActiveAction(row.id);
                // Recusa do backend (403, cadeira em uso) só ia para o log; sem
                // o toast a linha voltava igual, sem explicação.
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
              description="A cadeira sai do rodízio de atendimento. As sessões já registradas continuam no histórico."
              onConfirm={async () => {
                const result = await deleteChairAction(row.id);
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
    [toast, isPlatformTeam],
  );

  return (
    <>
      <InfiniteDataTable
        className="min-h-0 flex-1"
        title="Lista de cadeiras"
        columns={columns}
        initialSlice={initialSlice}
        loadPage={loadFilteredPage}
        reloadKey={reloadKey}
        getRowId={(row) => row.id}
        describe={(loaded, total) => `${loaded} de ${total} carregadas`}
        toolbar={
          <TableToolbar
            searchPlaceholder="Buscar por nome"
            searchValue={search}
            onSearchChange={setSearch}
            filter={<ChairsFilterModal value={filters} onApply={setFilters} />}
            action={<CreateChairModal onCreated={reload} firmwares={firmwares} />}
          />
        }
        emptyMessage={
          filtering
            ? "Nenhuma cadeira encontrada com esses critérios."
            : "Nenhuma cadeira cadastrada ainda."
        }
      />

      <ViewChairModal
        chair={viewing}
        isAdmin={isAdmin}
        onClose={() => setViewing(null)}
        onEdit={(chair) => {
          setViewing(null);
          setEditing(chair);
        }}
      />

      <EditChairModal
        chair={editing}
        onClose={() => setEditing(null)}
        onUpdated={reload}
        firmwares={firmwares}
      />
    </>
  );
}

/**
 * Estado do provisionamento de rede.
 *
 * <p>
 * Três situações que a tela precisa distinguir, e que um booleano juntaria:
 * nunca configurada, configurada e aplicada, e configurada mas rodando outra
 * rede — a última é a que dá trabalho, porque a cadeira parece bem até o AP
 * antigo sair do ar.
 */
function networkLabel(chair: Chair): string {
  if (chair.onConfiguredNetwork) return "Aplicada";
  if (chair.networkSyncedAt) return "Enviada";
  return "Não configurada";
}

function networkTone(chair: Chair): "success" | "warning" | "neutral" {
  if (chair.onConfiguredNetwork) return "success";
  if (chair.networkSyncedAt) return "warning";
  return "neutral";
}
