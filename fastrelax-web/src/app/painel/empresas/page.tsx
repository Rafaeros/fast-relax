import type { Metadata } from "next";
import { Alert } from "@/components/ui";
import { emptyPageSlice, toPageSlice } from "@/lib/api/pagination.types";
import { requirePlatformUser } from "@/features/authentication/lib/guards";
import { fetchCompaniesPage } from "@/features/companies/actions/company.actions";
import { CompaniesTable } from "@/features/companies/components/CompaniesTable";
import { listCompanies } from "@/features/companies/services/company.service";
import type { Company } from "@/features/companies/types/company.types";
import { listStates } from "@/features/locations/services/location.service";

export const metadata: Metadata = {
  title: "Empresas — physical",
};

export default async function EmpresasPage() {
  // Cadastro de cliente é da equipe da plataforma; quem opera uma empresa volta
  // para o próprio painel em vez de ver um 403 cru.
  await requirePlatformUser();

  // Primeira página no servidor: a tabela chega preenchida, sem piscar vazia.
  // As UFs vêm junto porque o formulário de endereço precisa delas ao abrir.
  const [result, states] = await Promise.all([listCompanies({ page: 0 }), listStates()]);
  const initialSlice = result.ok ? toPageSlice(result.data) : emptyPageSlice<Company>();

  return (
    // Altura de uma tela: a lista rola dentro da tabela, o resto fica parado.
    <div className="flex h-full min-h-0 flex-col gap-4">
      {!result.ok && (
        <Alert tone="error" title="Não foi possível carregar as empresas">
          {result.message}
        </Alert>
      )}

      <CompaniesTable
        initialSlice={initialSlice}
        states={states.ok ? states.data : []}
        loadPage={fetchCompaniesPage}
      />
    </div>
  );
}
