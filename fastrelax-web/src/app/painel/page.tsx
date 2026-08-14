import type { Metadata } from "next";
import { Alert } from "@/components/ui";
import { getCurrentUser } from "@/features/authentication/services/auth.service";
import { DashboardView } from "@/features/dashboard/components/DashboardView";
import { getDashboardSummary } from "@/features/dashboard/services/dashboard.service";
import { emptyDashboardSummary } from "@/features/dashboard/types/dashboard.types";

export const metadata: Metadata = {
  title: "Painel — physical",
};

export default async function PainelPage({ searchParams }: PageProps<"/painel">) {
  // Sem datas o backend assume os últimos 30 dias, mesmo padrão do filtro.
  const [user, params, result] = await Promise.all([
    getCurrentUser(),
    searchParams,
    getDashboardSummary(),
  ]);

  return (
    <div className="flex flex-col gap-8">
      {params.senha === "trocar" && (
        <Alert tone="warning" title="Senha temporária em uso">
          Defina uma senha definitiva para liberar o restante das funcionalidades.
        </Alert>
      )}

      <h1 className="font-display text-3xl text-ink-primary">
        Olá, {user?.name ?? "bem-vindo"}
      </h1>

      <DashboardView
        initialSummary={result.ok ? result.data : emptyDashboardSummary()}
        initialError={result.ok ? undefined : result.message}
      />
    </div>
  );
}
