"use client";

import { Badge, Button, DetailList, Modal } from "@/components/ui";
import type { DetailItem } from "@/components/ui";
import { formatCep, formatCnpj, formatLongDate, formatPhone } from "@/lib/format";
import type { Company } from "@/features/companies/types/company.types";

export type ViewCompanyModalProps = {
  /** `null` mantém o modal fechado — o pai guarda a linha selecionada. */
  company: Company | null;
  onClose: () => void;
  /** Leva o mesmo registro para a edição, sem fechar e reabrir manualmente. */
  onEdit: (company: Company) => void;
};

export function ViewCompanyModal({ company, onClose, onEdit }: ViewCompanyModalProps) {
  const items: DetailItem[] = company
    ? [
        { label: "Razão social", value: company.name, full: true },
        { label: "CNPJ", value: formatCnpj(company.cnpj) },
        {
          label: "Contrato",
          value: (
            <Badge tone={company.active ? "success" : "neutral"}>
              {company.active ? "Ativo" : "Suspenso"}
            </Badge>
          ),
        },
        { label: "E-mail", value: company.email },
        { label: "Telefone", value: formatPhone(company.phone) },
        { label: "Endereço", value: formatAddress(company), full: true },
        { label: "Rede das cadeiras", value: company.wifiSsid ?? "Não configurada" },
        {
          label: "Senha do Wi-Fi",
          // A senha em si não vem da API; o que o operador precisa saber é se
          // há uma guardada, e desde quando.
          value: company.wifiConfigured ? (
            <Badge tone="success">Guardada</Badge>
          ) : (
            <Badge tone="neutral">Ausente</Badge>
          ),
        },
        ...(company.wifiUpdatedAt
          ? [
              {
                label: "Rede atualizada em",
                value: formatLongDate(company.wifiUpdatedAt),
                full: true,
              } as DetailItem,
            ]
          : []),
        { label: "Cadastrada em", value: formatLongDate(company.createdAt), full: true },
      ]
    : [];

  return (
    <Modal
      open={Boolean(company)}
      onClose={onClose}
      size="sm"
      title="Detalhes da empresa"
      description="Suspender o contrato bloqueia o acesso de todo mundo dela."
      footer={
        <>
          <Button variant="ghost" size="sm" onClick={onClose}>
            Fechar
          </Button>
          {company && (
            <Button size="sm" onClick={() => onEdit(company)}>
              Editar
            </Button>
          )}
        </>
      }
    >
      {company && <DetailList items={items} />}
    </Modal>
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
