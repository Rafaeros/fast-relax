"use client";

import { useTransition } from "react";
import { Button, Icon, Input, MaskedInput, useToast } from "@/components/ui";
import { AddressFields } from "@/features/companies/components/AddressFields";
import { useCompanyDraft } from "@/features/companies/hooks/useCompanyDraft";
import { lookupCnpjAction } from "@/features/lookup/actions/lookup.actions";
import { hasValidCnpjCheckDigits } from "@/features/companies/schemas/company.schema";
import type { State } from "@/features/locations/types/location.types";
import type { Company, CompanyFieldErrors } from "@/features/companies/types/company.types";

export type CompanyFormFieldsProps = {
  states: State[];
  fieldErrors: CompanyFieldErrors;
  disabled?: boolean;
  /** Registro em edição — ausente no cadastro. */
  company?: Company;
};

/**
 * Campos da empresa, compartilhados entre cadastro e edição.
 *
 * <p>
 * O slug continua editável na edição porque é ele que o colaborador digita no
 * login: um cadastro com o valor errado deixa a empresa inteira sem acesso, e
 * a correção não pode depender de recriar o tenant.
 */
export function CompanyFormFields({
  states,
  fieldErrors,
  disabled,
  company,
}: CompanyFormFieldsProps) {
  const { draft, patch, applyAddress, resolvingCity } = useCompanyDraft({ states, company });
  const [lookingUp, startLookup] = useTransition();
  const toast = useToast();

  // Consultar com dígito verificador errado gasta uma chamada da cota da base
  // pública para receber 404 — e o erro real é de digitação, que a validação
  // local já sabe apontar.
  const cnpjReady = draft.cnpj.length === 14 && hasValidCnpjCheckDigits(draft.cnpj);

  const handleLookupCnpj = () => {
    startLookup(async () => {
      const result = await lookupCnpjAction(draft.cnpj);

      if (!result.ok) {
        toast.error(result.message);
        return;
      }

      const found = result.data;

      patch({
        name: found.name || draft.name,
        email: found.email || draft.email,
        phone: found.phone || draft.phone,
      });

      applyAddress(found.address, (applied) => {
        if (!applied.ok) {
          toast.error(applied.message);
          return;
        }

        // Situação cadastral não bloqueia: a Physical pode estar cadastrando um
        // cliente que acabou de regularizar, e a base leva dias para refletir.
        // Mas cadastrar uma empresa baixada sem perceber é pior.
        if (found.active) toast.success(result.message);
        else toast.error(`Atenção: situação cadastral "${found.status}" na Receita.`);
      });
    });
  };

  return (
    <>
      <div className="flex items-end gap-2">
        <MaskedInput
          name="cnpj"
          mask="cnpj"
          label="CNPJ"
          placeholder="00.000.000/0000-00"
          inputMode="numeric"
          autoFocus
          disabled={disabled}
          value={draft.cnpj}
          onValueChange={(digits) => patch({ cnpj: digits })}
          containerClassName="flex-1"
          hint="Identidade fiscal — o login usa o slug, logo abaixo."
          error={fieldErrors.cnpj}
          leadingIcon={<Icon name="building" />}
        />

        <Button
          type="button"
          variant="ghost"
          size="sm"
          className="mb-[26px]"
          disabled={disabled || lookingUp || !cnpjReady}
          onClick={handleLookupCnpj}
          leadingIcon={
            lookingUp ? (
              <Icon name="loader" className="h-4 w-4 animate-spin" />
            ) : (
              <Icon name="search" className="h-4 w-4" />
            )
          }
        >
          {lookingUp ? "Buscando..." : "Buscar"}
        </Button>
      </div>

      <Input
        name="name"
        label="Razão social"
        placeholder="Nome da empresa"
        maxLength={255}
        disabled={disabled}
        value={draft.name}
        onChange={(event) => patch({ name: event.target.value })}
        error={fieldErrors.name}
      />

      <Input
        name="slug"
        label="Slug (identificador de login)"
        placeholder="Deixe em branco para gerar a partir do nome"
        maxLength={60}
        disabled={disabled}
        value={draft.slug}
        onChange={(event) => patch({ slug: event.target.value.toLowerCase() })}
        hint="É o que os colaboradores informam na tela de login, em vez do CNPJ."
        error={fieldErrors.slug}
        leadingIcon={<Icon name="key" />}
      />

      <Input
        name="email"
        type="email"
        label="E-mail de contato"
        placeholder="contato@empresa.com"
        disabled={disabled}
        value={draft.email}
        onChange={(event) => patch({ email: event.target.value })}
        error={fieldErrors.email}
        leadingIcon={<Icon name="mail" />}
      />

      <MaskedInput
        name="phone"
        mask="phone"
        label="Telefone"
        placeholder="(00) 00000-0000"
        inputMode="numeric"
        disabled={disabled}
        value={draft.phone}
        onValueChange={(digits) => patch({ phone: digits })}
        error={fieldErrors.phone}
        leadingIcon={<Icon name="phone" />}
      />

      <div className="mt-1 border-t border-line pt-4">
        <p className="text-xs font-semibold uppercase tracking-[0.14em] text-ink-muted">
          Endereço
        </p>
      </div>

      <AddressFields
        states={states}
        fieldErrors={fieldErrors}
        disabled={disabled}
        draft={draft}
        patch={patch}
        applyAddress={applyAddress}
        resolvingCity={resolvingCity}
      />

      <div className="mt-1 border-t border-line pt-4">
        <p className="text-xs font-semibold uppercase tracking-[0.14em] text-ink-muted">
          Rede das cadeiras
        </p>
        <p className="mt-1 text-xs text-ink-muted">
          Gravada na memória do ESP32 pela ação &ldquo;Enviar configuração de
          rede&rdquo;, na lista de cadeiras. O BSSID fica em cada cadeira, para
          fixar o ponto de acesso quando houver mais de um.
        </p>
      </div>

      <Input
        name="wifiSsid"
        label="SSID"
        placeholder="Nome da rede Wi-Fi"
        maxLength={64}
        disabled={disabled}
        defaultValue={company?.wifiSsid ?? ""}
        hint="Limpar o SSID apaga também a senha guardada."
        error={fieldErrors.wifiSsid}
        leadingIcon={<Icon name="wrench" />}
      />

      <Input
        name="wifiPassword"
        type="password"
        label="Senha do Wi-Fi"
        // Sem `defaultValue`: a senha não volta da API por decisão de projeto,
        // e preencher com asteriscos falsos faria o campo mentir sobre o que
        // será enviado.
        placeholder={
          company?.wifiConfigured ? "Senha guardada — deixe em branco para manter" : "Senha da rede"
        }
        maxLength={128}
        autoComplete="new-password"
        disabled={disabled}
        hint={
          company?.wifiConfigured
            ? "Preencha só para trocar a senha. Em branco, a atual é mantida."
            : "Fica cifrada no banco e só sai daqui para o ESP32."
        }
        leadingIcon={<Icon name="lock" />}
      />
    </>
  );
}
