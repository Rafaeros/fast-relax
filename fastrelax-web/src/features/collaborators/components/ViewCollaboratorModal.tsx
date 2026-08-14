"use client";

import { Badge, Button, DetailList, Modal, TableIdentity } from "@/components/ui";
import type { DetailItem } from "@/components/ui";
import { formatCpf, formatLongDate, formatPhone } from "@/lib/format";
import { AllowedWindowsList } from "@/features/collaborators/components/AllowedWindowsList";
import { useCollaboratorSchedule } from "@/features/collaborators/hooks/useCollaboratorSchedule";
import type { Collaborator } from "@/features/collaborators/types/collaborator.types";

export type ViewCollaboratorModalProps = {
  /** `null` mantém o modal fechado — o pai guarda a linha selecionada. */
  collaborator: Collaborator | null;
  onClose: () => void;
  /** Leva o mesmo registro para a edição, sem fechar e reabrir manualmente. */
  onEdit: (collaborator: Collaborator) => void;
};

export function ViewCollaboratorModal({
  collaborator,
  onClose,
  onEdit,
}: ViewCollaboratorModalProps) {
  const { windows, loading, error } = useCollaboratorSchedule(collaborator?.id ?? null);

  const items: DetailItem[] = collaborator
    ? [
        { label: "CPF", value: formatCpf(collaborator.cpf) },
        { label: "Telefone", value: formatPhone(collaborator.phoneNumber) },
        { label: "Departamento", value: collaborator.departmentName ?? "—" },
        {
          label: "Situação",
          value: (
            <Badge tone={collaborator.active ? "success" : "neutral"}>
              {collaborator.active ? "Ativo" : "Inativo"}
            </Badge>
          ),
        },
        { label: "Cadastrado em", value: formatLongDate(collaborator.createdAt), full: true },
      ]
    : [];

  return (
    <Modal
      open={Boolean(collaborator)}
      onClose={onClose}
      title="Detalhes do colaborador"
      description="Dados usados pelo aplicativo e pela agenda de sessões."
      footer={
        <>
          <Button variant="ghost" size="sm" onClick={onClose}>
            Fechar
          </Button>
          {collaborator && (
            <Button size="sm" onClick={() => onEdit(collaborator)}>
              Editar
            </Button>
          )}
        </>
      }
    >
      {collaborator && (
        <div className="flex flex-col gap-6">
          <TableIdentity
            name={collaborator.name}
            secondary={collaborator.departmentName ?? undefined}
          />
          <DetailList items={items} />

          <div className="flex flex-col gap-3 border-t border-line pt-5">
            <span className="text-[0.6875rem] font-semibold uppercase tracking-[0.14em] text-ink-muted">
              Horário permitido
            </span>
            <AllowedWindowsList windows={windows} loading={loading} error={error} />
          </div>
        </div>
      )}
    </Modal>
  );
}
