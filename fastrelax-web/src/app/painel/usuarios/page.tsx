import type { Metadata } from "next";
import { Alert } from "@/components/ui";
import { emptyPageSlice, toPageSlice } from "@/lib/api/pagination.types";
import { requirePanelUser } from "@/features/authentication/lib/guards";
import { listCompanies } from "@/features/companies/services/company.service";
import { fetchUsersPage } from "@/features/users/actions/user.actions";
import { UsersTable } from "@/features/users/components/UsersTable";
import { listUsers } from "@/features/users/services/user.service";
import type { CompanyOption, User } from "@/features/users/types/user.types";

export const metadata: Metadata = {
  title: "Usuários — physical",
};

export default async function UsuariosPage() {
  // O RH não administra contas do painel: quem cadastra usuário é o gestor da
  // empresa ou a equipe da plataforma.
  const user = await requirePanelUser(["SYSADMIN", "COMPANY_ADMIN"]);

  // Primeira página no servidor: a tabela chega preenchida, sem piscar vazia.
  const result = await listUsers({ page: 0 });
  const initialSlice = result.ok ? toPageSlice(result.data) : emptyPageSlice<User>();

  // A lista de empresas só faz sentido para o SYSADMIN, que cadastra o gestor
  // de cada cliente. Para os demais o backend usa a empresa do contexto, então
  // buscar aqui seria uma chamada garantida a tomar 403.
  const companies: CompanyOption[] =
    user.role === "SYSADMIN" ? await loadCompanyOptions() : [];

  return (
    // Altura de uma tela: a lista rola dentro da tabela, o resto fica parado.
    <div className="flex h-full min-h-0 flex-col gap-4">
      {!result.ok && (
        <Alert tone="error" title="Não foi possível carregar os usuários">
          {result.message}
        </Alert>
      )}

      <UsersTable
        initialSlice={initialSlice}
        loadPage={fetchUsersPage}
        currentRole={user.role}
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
