"use client";

import { Badge, Button, DetailList, Modal } from "@/components/ui";
import type { DetailItem } from "@/components/ui";
import { formatLongDate } from "@/lib/format";
import type { Department } from "@/features/departments/types/department.types";

export type ViewDepartmentModalProps = {
  /** `null` mantém o modal fechado — o pai guarda a linha selecionada. */
  department: Department | null;
  onClose: () => void;
  /** Leva o mesmo registro para a edição, sem fechar e reabrir manualmente. */
  onEdit: (department: Department) => void;
};

export function ViewDepartmentModal({
  department,
  onClose,
  onEdit,
}: ViewDepartmentModalProps) {
  const items: DetailItem[] = department
    ? [
        { label: "Nome", value: department.name },
        {
          label: "Situação",
          value: (
            <Badge tone={department.active ? "success" : "neutral"}>
              {department.active ? "Ativo" : "Inativo"}
            </Badge>
          ),
        },
        { label: "Cadastrado em", value: formatLongDate(department.createdAt), full: true },
      ]
    : [];

  return (
    <Modal
      open={Boolean(department)}
      onClose={onClose}
      size="sm"
      title="Detalhes do departamento"
      description="Usado para agrupar colaboradores nos indicadores de uso."
      footer={
        <>
          <Button variant="ghost" size="sm" onClick={onClose}>
            Fechar
          </Button>
          {department && (
            <Button size="sm" onClick={() => onEdit(department)}>
              Editar
            </Button>
          )}
        </>
      }
    >
      {department && <DetailList items={items} />}
    </Modal>
  );
}
