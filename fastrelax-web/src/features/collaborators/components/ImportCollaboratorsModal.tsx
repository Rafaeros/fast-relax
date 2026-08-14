"use client";

import { useState, useTransition, type FormEvent } from "react";
import { Alert, Button, Card, Icon, Input, Modal, Stat, buttonStyles } from "@/components/ui";
import { importCollaboratorsAction } from "@/features/collaborators/actions/import.actions";
import {
  IMPORT_ACCEPT,
  IMPORT_INITIAL_STATE,
  type ImportResult,
} from "@/features/collaborators/types/import.types";

export type ImportCollaboratorsModalProps = {
  /** Disparado ao fechar depois de um arquivo processado — a tabela recarrega. */
  onImported: () => void;
};

export function ImportCollaboratorsModal({ onImported }: ImportCollaboratorsModalProps) {
  const [open, setOpen] = useState(false);
  const [state, setState] = useState(IMPORT_INITIAL_STATE);
  const [pending, startTransition] = useTransition();
  // Trocar a key remonta o input de arquivo para a próxima importação.
  const [formKey, setFormKey] = useState(0);

  const result = state.result;

  const reset = () => {
    setState(IMPORT_INITIAL_STATE);
    setFormKey((current) => current + 1);
  };

  const openModal = () => {
    reset();
    setOpen(true);
  };

  const close = () => {
    setOpen(false);
    // Recarrega só quando algo foi de fato processado.
    if (result && result.processed > 0) onImported();
    reset();
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);

    startTransition(async () => {
      setState(await importCollaboratorsAction(state, formData));
    });
  };

  return (
    <>
      <Button
        variant="secondary"
        size="sm"
        onClick={openModal}
        leadingIcon={<Icon name="upload" className="h-4 w-4" />}
      >
        Importar
      </Button>

      <Modal
        open={open}
        onClose={close}
        dismissible={!pending}
        title="Importar colaboradores"
        description="Cadastra os novos e atualiza os existentes pelo CPF, junto do horário permitido."
        footer={
          result ? (
            <Button size="sm" onClick={close}>
              Concluir
            </Button>
          ) : (
            <>
              <Button variant="ghost" size="sm" onClick={close} disabled={pending}>
                Cancelar
              </Button>
              <Button
                type="submit"
                form="import-collaborators-form"
                size="sm"
                disabled={pending}
                trailingIcon={
                  pending ? <Icon name="loader" className="h-4 w-4 animate-spin" /> : undefined
                }
              >
                {pending ? "Importando..." : "Importar planilha"}
              </Button>
            </>
          )
        }
      >
        {result ? (
          <ImportSummary result={result} />
        ) : (
          <form
            key={formKey}
            id="import-collaborators-form"
            onSubmit={handleSubmit}
            className="flex flex-col gap-5"
          >
            {state.status === "error" && state.message && (
              <Alert tone="error">{state.message}</Alert>
            )}

            <Alert tone="info" title="Como funciona">
              Linhas com erro não interrompem o arquivo: são puladas e listadas ao final, para
              corrigir e reenviar só o que falhou.
            </Alert>

            <Input
              name="file"
              type="file"
              accept={IMPORT_ACCEPT}
              label="Planilha"
              hint="Formato .xlsx, no mesmo layout do modelo."
              required
              disabled={pending}
              className="file:mr-3 file:rounded-control file:border-0 file:bg-surface-hover file:px-3 file:py-1.5 file:text-xs file:font-semibold file:text-ink-primary"
            />

            <a
              href="/api/colaboradores/planilha?tipo=modelo"
              className={buttonStyles({ variant: "ghost", size: "sm", className: "self-start" })}
            >
              <Icon name="sheet" className="h-4 w-4" />
              Baixar planilha modelo
            </a>
          </form>
        )}
      </Modal>
    </>
  );
}

function ImportSummary({ result }: { result: ImportResult }) {
  const metrics = [
    { value: String(result.collaboratorsCreated), label: "Cadastrados" },
    { value: String(result.collaboratorsUpdated), label: "Atualizados" },
    { value: String(result.departmentsCreated), label: "Departamentos criados" },
    { value: String(result.schedulesSaved), label: "Horários salvos" },
  ];

  return (
    <div className="flex flex-col gap-5">
      {result.failed === 0 ? (
        <Alert tone="success" title="Importação concluída">
          {result.processed} de {result.totalRows} linhas processadas.
        </Alert>
      ) : (
        <Alert tone="warning" title={`${result.failed} linha(s) com erro`}>
          As demais foram importadas. Corrija as linhas abaixo na planilha e reenvie só elas.
        </Alert>
      )}

      <div className="grid grid-cols-2 gap-3">
        {metrics.map((metric) => (
          <Card key={metric.label} padding="sm">
            <Stat value={metric.value} label={metric.label} />
          </Card>
        ))}
      </div>

      {result.errors.length > 0 && (
        <div className="flex flex-col gap-2">
          <span className="text-xs font-semibold tracking-wide text-ink-secondary">
            Linhas recusadas
          </span>

          <div className="max-h-56 overflow-auto rounded-control border border-line">
            <table className="w-full text-left text-xs">
              <thead className="sticky top-0 bg-surface-card text-ink-muted">
                <tr>
                  <th className="px-3 py-2 font-semibold">Linha</th>
                  <th className="px-3 py-2 font-semibold">Nome</th>
                  <th className="px-3 py-2 font-semibold">Motivo</th>
                </tr>
              </thead>
              <tbody>
                {result.errors.map((error) => (
                  <tr
                    key={`${error.row}-${error.reason}`}
                    className="border-t border-line-soft/60 text-ink-secondary"
                  >
                    <td className="px-3 py-2 tabular-nums">{error.row}</td>
                    <td className="px-3 py-2">{error.name ?? "—"}</td>
                    <td className="px-3 py-2">{error.reason}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
