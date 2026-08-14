import type { Metadata } from "next";
import { Alert } from "@/components/ui";
import { emptyPageSlice, toPageSlice } from "@/lib/api/pagination.types";
import { fetchDepartmentsPage } from "@/features/departments/actions/department.actions";
import { DepartmentsTable } from "@/features/departments/components/DepartmentsTable";
import { listDepartments } from "@/features/departments/services/department.service";
import type { Department } from "@/features/departments/types/department.types";

export const metadata: Metadata = {
  title: "Departamentos — physical",
};

export default async function DepartamentosPage() {
  // Primeira página no servidor: a tabela chega preenchida, sem piscar vazia.
  const result = await listDepartments({ page: 0 });
  const initialSlice = result.ok ? toPageSlice(result.data) : emptyPageSlice<Department>();

  return (
    // Altura de uma tela: a lista rola dentro da tabela, o resto fica parado.
    <div className="flex h-full min-h-0 flex-col gap-4">
      {!result.ok && (
        <Alert tone="error" title="Não foi possível carregar os departamentos">
          {result.message}
        </Alert>
      )}

      <DepartmentsTable initialSlice={initialSlice} loadPage={fetchDepartmentsPage} />
    </div>
  );
}
