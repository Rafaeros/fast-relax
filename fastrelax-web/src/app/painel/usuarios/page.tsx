import type { Metadata } from "next";
import { Alert } from "@/components/ui";
import { emptyPageSlice, toPageSlice } from "@/lib/api/pagination.types";
import { fetchUsersPage } from "@/features/users/actions/user.actions";
import { UsersTable } from "@/features/users/components/UsersTable";
import { listUsers } from "@/features/users/services/user.service";
import type { User } from "@/features/users/types/user.types";

export const metadata: Metadata = {
  title: "Usuários — physical",
};

export default async function UsuariosPage() {
  // Primeira página no servidor: a tabela chega preenchida, sem piscar vazia.
  const result = await listUsers({ page: 0 });
  const initialSlice = result.ok ? toPageSlice(result.data) : emptyPageSlice<User>();

  return (
    // Altura de uma tela: a lista rola dentro da tabela, o resto fica parado.
    <div className="flex h-full min-h-0 flex-col gap-4">
      {!result.ok && (
        <Alert tone="error" title="Não foi possível carregar os usuários">
          {result.message}
        </Alert>
      )}

      <UsersTable initialSlice={initialSlice} loadPage={fetchUsersPage} />
    </div>
  );
}
