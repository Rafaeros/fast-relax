import {
  Alert,
  Badge,
  Card,
  CardBody,
  CardDescription,
  CardTitle,
  CopyField,
  DetailList,
} from "@/components/ui";
import type { DetailItem } from "@/components/ui";
import { formatCep, formatCnpj, formatPhone } from "@/lib/format";
import type { Company } from "@/features/companies/types/company.types";

export type MyCompanyViewProps = {
  company: Company | null;
  /** Mensagem da API quando a leitura falhou no servidor. */
  error?: string;
};

/**
 * Leitura da própria empresa para o RH/admin do cliente — sem edição.
 *
 * <p>
 * O slug é o único dado aqui que muda o dia a dia de todo mundo: é o que cada
 * colaborador digita para entrar. Corrigi-lo é a Physical quem faz, por isso
 * fica só em exibição — trocado sem avisar a equipe, derruba o login de todos
 * até o RH descobrir o motivo.
 */
export function MyCompanyView({ company, error }: MyCompanyViewProps) {
  if (error || !company) {
    return (
      <Alert tone="error" title="Não foi possível carregar os dados da empresa">
        {error ?? "Tente novamente em instantes."}
      </Alert>
    );
  }

  const items: DetailItem[] = [
    { label: "Razão social", value: company.name, full: true },
    { label: "CNPJ", value: formatCnpj(company.cnpj) },
    { label: "E-mail de contato", value: company.email },
    { label: "Telefone", value: formatPhone(company.phone) },
    { label: "Endereço", value: formatAddress(company), full: true },
    {
      label: "Contrato",
      value: (
        <Badge tone={company.active ? "success" : "neutral"}>
          {company.active ? "Ativo" : "Suspenso"}
        </Badge>
      ),
    },
  ];

  return (
    <div className="flex flex-col gap-6">
      <Card variant="elevated" padding="lg">
        <CardBody>
          <CardTitle>Identificador de login</CardTitle>
          <CardDescription>
            É isto que cada colaborador digita na tela de entrada, em vez do CNPJ. Para
            trocar, peça à Physical — o valor é o mesmo para todo mundo da empresa.
          </CardDescription>
          <CopyField value={company.slug} className="mt-2 max-w-xs" />
        </CardBody>
      </Card>

      <Card padding="lg">
        <CardBody>
          <CardTitle>Dados cadastrais</CardTitle>
          <DetailList items={items} className="mt-2" />
        </CardBody>
      </Card>
    </div>
  );
}

/** Linha única de endereço; partes ausentes somem em vez de virar travessão. */
function formatAddress(company: Company): string {
  const line = [company.street, company.number, company.complement].filter(Boolean).join(", ");
  const city = [company.cityName, company.stateAcronym].filter(Boolean).join("/");
  const cep = company.cep ? `CEP ${formatCep(company.cep)}` : "";

  const parts = [line, city, cep].filter(Boolean);
  return parts.length > 0 ? parts.join(" — ") : "—";
}
