import type { Metadata } from "next";
import { Alert } from "@/components/ui";
import { emptyPageSlice, toPageSlice } from "@/lib/api/pagination.types";
import { fetchChairsPage } from "@/features/chairs/actions/chair.actions";
import { ChairsTable } from "@/features/chairs/components/ChairsTable";
import { listChairs } from "@/features/chairs/services/chair.service";
import { requirePanelUser } from "@/features/authentication/lib/guards";
import { administersCompany, isPlatformTeam } from "@/features/authentication/lib/roles";
import { listCompanies } from "@/features/companies/services/company.service";
import { listFirmwareOptions } from "@/features/firmwares/services/firmware.service";
import type { Chair, CompanyOption, FirmwareOption } from "@/features/chairs/types/chair.types";

export const metadata: Metadata = {
  title: "Cadeiras — physical",
};

export default async function CadeirasPage() {
  // Os três papéis entram, por motivos diferentes: a empresa opera o próprio
  // parque, e a equipe da plataforma enxerga o de todos os clientes porque é
  // ela quem instala o equipamento e configura a rede dele. Cadeira é ativo da
  // Physical — colaborador e sessão continuam fora do alcance dela.
  const user = await requirePanelUser(["SYSADMIN", "COMPANY_ADMIN", "COMPANY_RH"]);
  const platform = isPlatformTeam(user);

  // Primeira página no servidor: a tabela chega preenchida, sem piscar vazia.
  // As versões vêm junto porque o formulário precisa delas ao abrir.
  const [result, firmwares] = await Promise.all([listChairs({ page: 0 }), listFirmwareOptions()]);
  const initialSlice = result.ok ? toPageSlice(result.data) : emptyPageSlice<Chair>();

  const firmwareOptions: FirmwareOption[] = firmwares.ok
    ? firmwares.data.content.map((firmware) => ({
        id: firmware.id,
        version: firmware.version,
        productName: firmware.productName,
      }))
    : [];

  // Só a equipe da plataforma cadastra cadeira, e só ela escolhe a empresa
  // dona do equipamento — para os demais buscar aqui seria uma chamada
  // garantida a tomar 403.
  const companies: CompanyOption[] = platform ? await loadCompanyOptions() : [];

  return (
    // Altura de uma tela: a lista rola dentro da tabela, o resto fica parado.
    <div className="flex h-full min-h-0 flex-col gap-4">
      {!result.ok && (
        <Alert tone="error" title="Não foi possível carregar as cadeiras">
          {result.message}
        </Alert>
      )}

      <ChairsTable
        initialSlice={initialSlice}
        loadPage={fetchChairsPage}
        // O teste de relé aciona o equipamento de verdade: fica com o gestor.
        isAdmin={administersCompany(user) || platform}
        isPlatformTeam={platform}
        firmwares={firmwareOptions}
        companies={companies}
      />
    </div>
  );
}

/** Só id e nome chegam ao cliente: o select não precisa do cadastro inteiro. */
async function loadCompanyOptions(): Promise<CompanyOption[]> {
  const result = await listCompanies({ page: 0, size: 200 });
  if (!result.ok) return [];

  return result.data.content.map((company) => ({ id: company.id, name: company.name }));
}
