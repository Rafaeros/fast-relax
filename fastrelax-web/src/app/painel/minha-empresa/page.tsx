import type { Metadata } from "next";
import { MyCompanyView } from "@/features/companies/components/MyCompanyView";
import { getMyCompany } from "@/features/companies/services/company.service";
import { requireCompanyUser } from "@/features/authentication/lib/guards";

export const metadata: Metadata = {
  title: "Minha empresa — physical",
};

export default async function MinhaEmpresaPage() {
  // Cadastro é da Physical (evita CNPJ ou slug errados derrubando o acesso de
  // todo mundo). O RH/admin do cliente só consulta, principalmente o slug que
  // os colaboradores digitam no login.
  await requireCompanyUser();

  const result = await getMyCompany();

  return <MyCompanyView company={result.ok ? result.data : null} error={result.ok ? undefined : result.message} />;
}
