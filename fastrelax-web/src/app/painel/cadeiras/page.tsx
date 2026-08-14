import type { Metadata } from "next";
import { Alert } from "@/components/ui";
import { emptyPageSlice, toPageSlice } from "@/lib/api/pagination.types";
import { fetchChairsPage } from "@/features/chairs/actions/chair.actions";
import { ChairsTable } from "@/features/chairs/components/ChairsTable";
import { listChairs } from "@/features/chairs/services/chair.service";
import type { Chair } from "@/features/chairs/types/chair.types";

export const metadata: Metadata = {
  title: "Cadeiras — physical",
};

export default async function CadeirasPage() {
  // Primeira página no servidor: a tabela chega preenchida, sem piscar vazia.
  const result = await listChairs({ page: 0 });
  const initialSlice = result.ok ? toPageSlice(result.data) : emptyPageSlice<Chair>();

  return (
    // Altura de uma tela: a lista rola dentro da tabela, o resto fica parado.
    <div className="flex h-full min-h-0 flex-col gap-4">
      {!result.ok && (
        <Alert tone="error" title="Não foi possível carregar as cadeiras">
          {result.message}
        </Alert>
      )}

      <ChairsTable initialSlice={initialSlice} loadPage={fetchChairsPage} />
    </div>
  );
}
