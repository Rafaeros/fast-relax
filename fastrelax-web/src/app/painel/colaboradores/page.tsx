import type { Metadata } from "next";
import { Alert } from "@/components/ui";
import { toPageSlice, emptyPageSlice } from "@/lib/api/pagination.types";
import { fetchCollaboratorsPage } from "@/features/collaborators/actions/collaborator.actions";
import { CollaboratorsTable } from "@/features/collaborators/components/CollaboratorsTable";
import { listCollaborators } from "@/features/collaborators/services/collaborator.service";
import type { Collaborator } from "@/features/collaborators/types/collaborator.types";
import { listActiveDepartments } from "@/features/departments/services/department.service";

export const metadata: Metadata = {
  title: "Colaboradores — physical",
};

export default async function ColaboradoresPage() {
  // Primeira página no servidor: a tabela chega preenchida, sem piscar vazia.
  // Departamentos vêm junto porque o cadastro exige um (`departmentId` é @NotNull).
  const [result, departmentsResult] = await Promise.all([
    listCollaborators({ page: 0 }),
    listActiveDepartments(),
  ]);

  const initialSlice = result.ok ? toPageSlice(result.data) : emptyPageSlice<Collaborator>();
  const departments = departmentsResult.ok ? departmentsResult.data.content : [];

  return (
    // Altura de uma tela: a lista rola dentro da tabela, o resto fica parado.
    <div className="flex h-full min-h-0 flex-col gap-4">
      {!result.ok && (
        <Alert tone="error" title="Não foi possível carregar os colaboradores">
          {result.message}
        </Alert>
      )}

      <CollaboratorsTable
        initialSlice={initialSlice}
        loadPage={fetchCollaboratorsPage}
        departments={departments}
      />
    </div>
  );
}
