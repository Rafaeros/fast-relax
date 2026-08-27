"use client";

import { Icon, Input, Textarea } from "@/components/ui";
import type {
  Firmware,
  FirmwareFieldErrors,
} from "@/features/firmwares/types/firmware.types";

export type FirmwareFormFieldsProps = {
  fieldErrors: FirmwareFieldErrors;
  disabled?: boolean;
  /** Registro em edição — ausente no cadastro. */
  firmware?: Firmware;
};

/**
 * Campos do firmware, compartilhados entre cadastro e edição.
 *
 * <p>
 * Só metadados. O binário sobe por upload — no cadastro, pelo seletor logo
 * abaixo destes campos; depois, pela tela de detalhes. Nome, tamanho e SHA-256
 * saem do próprio arquivo no servidor, e não há o que digitar sobre eles aqui.
 */
export function FirmwareFormFields({
  fieldErrors,
  disabled,
  firmware,
}: FirmwareFormFieldsProps) {
  return (
    <>
      <Input
        name="productName"
        label="Produto"
        placeholder="Cadeira FastRelax"
        maxLength={100}
        autoFocus
        disabled={disabled}
        defaultValue={firmware?.productName}
        error={fieldErrors.productName}
        leadingIcon={<Icon name="chair" />}
      />

      <Input
        name="version"
        label="Versão"
        placeholder="1.4.0"
        maxLength={50}
        disabled={disabled}
        defaultValue={firmware?.version}
        hint="Única no catálogo — é como o suporte identifica o que está em campo."
        error={fieldErrors.version}
      />

      <Input
        name="releaseDate"
        type="date"
        label="Data de publicação"
        disabled={disabled}
        defaultValue={firmware?.releaseDate}
        error={fieldErrors.releaseDate}
      />

      <Textarea
        name="releaseNotes"
        label="Notas da versão"
        placeholder="O que mudou nesta versão (opcional)"
        rows={3}
        disabled={disabled}
        defaultValue={firmware?.releaseNotes ?? ""}
      />

      {/* Na edição não há anexo: o gerenciamento dos binários — baixar, remover,
          gravar na placa — mora na tela de detalhes, onde a versão já existe. */}
      {firmware && (
        <p className="border-t border-line pt-4 text-xs text-ink-tertiary">
          Os binários desta versão são gerenciados na tela de detalhes.
        </p>
      )}
    </>
  );
}
