"use client";

import { Icon, Input, Select } from "@/components/ui";
import type {
  Chair,
  ChairFieldErrors,
  CompanyOption,
  FirmwareOption,
} from "@/features/chairs/types/chair.types";


export type ChairFormFieldsProps = {
  fieldErrors: ChairFieldErrors;
  disabled?: boolean;
  /** Registro em edição — ausente no cadastro. */
  chair?: Chair;
  /** Versões do catálogo da Physical, para registrar o que está gravado. */
  firmwares?: FirmwareOption[];
  /**
   * Empresas para o select de dono do equipamento — aparece no cadastro e na
   * edição completa (Physical pode reatribuir a cadeira a outro cliente).
   */
  companies?: CompanyOption[];
};

/**
 * Campos da cadeira, compartilhados entre cadastro e edição.
 *
 * O MAC é a identidade do dispositivo; IP e porta são apenas endereço e ficam
 * opcionais porque o heartbeat do ESP32 os preenche sozinho.
 */
export function ChairFormFields({
  fieldErrors,
  disabled,
  chair,
  firmwares = [],
  companies,
}: ChairFormFieldsProps) {
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

      {companies && (
        <Select
          name="companyId"
          label="Empresa"
          defaultValue={chair?.companyId ? String(chair.companyId) : ""}
          disabled={disabled}
          error={fieldErrors.companyId}
          hint={
            chair
              ? "Trocar aqui move a cadeira para outro cliente — ela some da lista de quem a tinha."
              : "Só ela vai enxergar e operar esta cadeira."
          }
          options={[
            { label: "Selecione a empresa", value: "" },
            ...companies.map((company) => ({ label: company.name, value: String(company.id) })),
          ]}
        />
      )}

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

      {/*
        A versão é informativa: quem grava o firmware é a Physical, pela porta
        do dispositivo. Registrar aqui é o que permite ao suporte saber o que
        está em campo sem ir até a cadeira.
      */}
      {/*
        Fixar o AP importa em planta com vários pontos no mesmo SSID: sem isso a
        cadeira pode grudar num ponto distante e ficar com sinal ruim tendo um
        AP a três metros. Em branco, o ESP32 escolhe o de melhor sinal.
      */}
      <Input
        name="wifiBssid"
        label="BSSID do ponto de acesso"
        placeholder="AA:BB:CC:DD:EE:FF"
        autoComplete="off"
        maxLength={17}
        disabled={disabled}
        defaultValue={chair?.wifiBssid ?? ""}
        hint="Opcional — em branco, a cadeira escolhe o AP de melhor sinal."
        error={fieldErrors.wifiBssid}
        leadingIcon={<Icon name="wrench" />}
      />

      {/*
        Override raro: a maioria das cadeiras nunca precisa sair do broker
        padrão (mesmo host/usuário/senha que já vem embutido em config.h). Em
        branco no host apaga qualquer override e volta a valer o padrão
        global — mesmo critério do BSSID acima.
      */}
      <div className="grid gap-5 sm:grid-cols-[1fr_140px]">
        <Input
          name="mqttHost"
          label="Broker MQTT (opcional)"
          placeholder="Em branco usa o broker padrão"
          autoComplete="off"
          maxLength={255}
          disabled={disabled}
          defaultValue={chair?.mqttHost ?? ""}
          hint="Só preencha se esta cadeira específica precisar de outro broker."
          error={fieldErrors.mqttHost}
          leadingIcon={<Icon name="wrench" />}
        />

        <Input
          name="mqttPort"
          label="Porta"
          type="number"
          min={1}
          max={65535}
          placeholder="1883"
          autoComplete="off"
          disabled={disabled}
          defaultValue={chair?.mqttPort ?? ""}
          error={fieldErrors.mqttPort}
        />
      </div>

      <div className="grid gap-5 sm:grid-cols-2">
        <Input
          name="mqttUsername"
          label="Usuário do broker"
          placeholder="Opcional"
          autoComplete="off"
          maxLength={100}
          disabled={disabled}
          defaultValue={chair?.mqttUsername ?? ""}
          error={fieldErrors.mqttUsername}
        />

        <Input
          name="mqttPassword"
          label="Senha do broker"
          type="password"
          placeholder={chair?.mqttHost ? "Deixe em branco para manter a atual" : "Opcional"}
          autoComplete="new-password"
          disabled={disabled}
          hint="Nunca é exibida de volta — em branco mantém a senha já gravada."
          error={fieldErrors.mqttPassword}
        />
      </div>

      <Select
        name="firmwareId"
        label="Firmware instalado"
        defaultValue={chair?.firmwareId ? String(chair.firmwareId) : ""}
        disabled={disabled}
        error={fieldErrors.firmwareId}
        hint="Opcional — deixe em branco se a versão não for conhecida."
        options={[
          { label: "Não informado", value: "" },
          ...firmwares.map((firmware) => ({
            label: `${firmware.productName} — ${firmware.version}`,
            value: String(firmware.id),
          })),
        ]}
      />
    </>
  );
}
