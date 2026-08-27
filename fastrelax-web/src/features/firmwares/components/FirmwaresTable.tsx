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
import { deleteFirmwareAction } from "@/features/firmwares/actions/firmware.actions";
import { CreateFirmwareModal } from "@/features/firmwares/components/CreateFirmwareModal";
import { EditFirmwareModal } from "@/features/firmwares/components/EditFirmwareModal";
import { ViewFirmwareModal } from "@/features/firmwares/components/ViewFirmwareModal";
import type { Firmware } from "@/features/firmwares/types/firmware.types";

export type FirmwaresTableProps = {
  initialSlice: PageSlice<Firmware>;
  loadPage: (page: number) => Promise<PageSlice<Firmware>>;
};

export function FirmwaresTable({ initialSlice, loadPage }: FirmwaresTableProps) {
  // Incrementar o sinal faz a tabela descartar o que está em tela e recarregar
  // da primeira página — é assim que publicação e edição aparecem na hora.
  const [reloadSignal, setReloadSignal] = useState(0);
  const [search, setSearch] = useState("");
  const debouncedSearch = useDebouncedValue(search);
  // Um modal por tela, não um por linha: a lista cresce com a rolagem infinita
  // e montar um diálogo por registro encheria o DOM à toa.
  const [viewing, setViewing] = useState<Firmware | null>(null);
  const [editing, setEditing] = useState<Firmware | null>(null);
  const toast = useToast();

  const reload = () => setReloadSignal((current) => current + 1);

  /**
   * `GET /firmwares` aceita só paginação — não há filtro no servidor. A busca
   * roda sobre as linhas já carregadas; a rolagem continua trazendo páginas,
   * então descer amplia o conjunto pesquisado.
   */
  const filterRow = useCallback(
    (firmware: Firmware) => {
      const term = debouncedSearch.trim().toLowerCase();
      if (!term) return true;

      return `${firmware.productName} ${firmware.version}`.toLowerCase().includes(term);
    },
    [debouncedSearch],
  );

  const filtering = debouncedSearch.trim().length > 0;

  /**
   * As colunas vivem aqui porque `cell` é função — o servidor não consegue
   * serializar isso para um componente cliente — e porque precisam abrir os
   * modais, que são estado desta tabela.
   */
  const columns = useMemo<DataTableColumn<Firmware>[]>(
    () => [
      {
        id: "version",
        header: "Versão",
        cell: (row) => <TableIdentity name={row.version} secondary={row.productName} />,
      },
      {
        id: "releaseDate",
        header: "Publicado em",
        cell: (row) => (
          <span className="text-ink-secondary">{formatLongDate(row.releaseDate)}</span>
        ),
      },
      {
        id: "files",
        header: "Binários",
        hideOnMobile: true,
        cell: (row) => (
          <Badge tone={row.files.length > 0 ? "neutral" : "warning"}>
            {row.files.length > 0 ? `${row.files.length} arquivo(s)` : "Sem binário"}
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
              itemName={row.version}
              description="As cadeiras que já rodam esta versão continuam registradas nela."
              onConfirm={async () => {
                const result = await deleteFirmwareAction(row.id);
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
        title="Catálogo de firmwares"
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
            searchPlaceholder="Buscar por versão ou produto"
            searchValue={search}
            onSearchChange={setSearch}
            action={<CreateFirmwareModal onCreated={reload} />}
          />
        }
        emptyMessage={
          filtering
            ? "Nenhuma versão encontrada com esses critérios."
            : "Nenhuma versão publicada ainda."
        }
      />

      <ViewFirmwareModal
        firmware={viewing}
        onClose={() => setViewing(null)}
        onEdit={(firmware) => {
          setViewing(null);
          setEditing(firmware);
        }}
        // A página inteira é protegida por `requirePlatformUser`: quem chega
        // aqui já é da equipe da plataforma.
        canManage
        onChanged={reload}
      />

      <EditFirmwareModal
        firmware={editing}
        onClose={() => setEditing(null)}
        onUpdated={reload}
      />
    </>
  );
}
