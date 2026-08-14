"use client";

import { Icon, Input } from "@/components/ui";
import type { Chair, ChairFieldErrors } from "@/features/chairs/types/chair.types";

export type ChairFormFieldsProps = {
  fieldErrors: ChairFieldErrors;
  disabled?: boolean;
  /** Registro em edição — ausente no cadastro. */
  chair?: Chair;
};

/**
 * Campos da cadeira, compartilhados entre cadastro e edição.
 *
 * O MAC é a identidade do dispositivo; IP e porta são apenas endereço e ficam
 * opcionais porque o heartbeat do ESP32 os preenche sozinho.
 */
export function ChairFormFields({ fieldErrors, disabled, chair }: ChairFormFieldsProps) {
  return (
    <>
      <Input
        name="name"
        label="Nome da cadeira"
        placeholder="Ex.: Sala de descanso 1"
        autoComplete="off"
        autoFocus
        maxLength={100}
        disabled={disabled}
        defaultValue={chair?.name}
        error={fieldErrors.name}
        leadingIcon={<Icon name="chair" />}
      />

      <Input
        name="macAddress"
        label="MAC address do ESP32"
        placeholder="AA:BB:CC:DD:EE:FF"
        autoComplete="off"
        maxLength={17}
        disabled={disabled}
        defaultValue={chair?.macAddress}
        error={fieldErrors.macAddress}
        hint="Identifica o dispositivo. Trocar a placa exige atualizar aqui."
        leadingIcon={<Icon name="key" />}
      />

      <div className="grid gap-5 sm:grid-cols-[1fr_140px]">
        <Input
          name="ipAddress"
          label="IP na rede (opcional)"
          placeholder="192.168.1.50"
          autoComplete="off"
          maxLength={45}
          disabled={disabled}
          defaultValue={chair?.ipAddress ?? ""}
          error={fieldErrors.ipAddress}
          hint="Preenchido sozinho quando o ESP32 se anuncia."
          leadingIcon={<Icon name="wrench" />}
        />

        <Input
          name="port"
          label="Porta"
          type="number"
          min={1}
          max={65535}
          placeholder="80"
          autoComplete="off"
          disabled={disabled}
          defaultValue={chair?.port ?? 80}
          error={fieldErrors.port}
        />
      </div>
    </>
  );
}
