"use client";

import { useEffect, useRef, useState, useTransition } from "react";
import { Button, Icon, Input, MaskedInput, Select, useToast } from "@/components/ui";
import { createCityAction, fetchCitiesAction } from "@/features/locations/actions/location.actions";
import { lookupCepAction } from "@/features/lookup/actions/lookup.actions";
import type { City, State } from "@/features/locations/types/location.types";
import type { CompanyFieldErrors } from "@/features/companies/types/company.types";
import type { CompanyDraft } from "@/features/companies/hooks/useCompanyDraft";
import type { LookupAddress } from "@/features/lookup/types/lookup.types";

export type AddressFieldsProps = {
  states: State[];
  fieldErrors: CompanyFieldErrors;
  disabled?: boolean;
  draft: CompanyDraft;
  patch: (changes: Partial<CompanyDraft>) => void;
  /** Preenche o endereço a partir de uma consulta — compartilhado com o CNPJ. */
  applyAddress: (
    address: LookupAddress,
    onDone?: (result: { ok: boolean; message: string }) => void,
  ) => void;
  /** Uma cidade está sendo resolvida pela consulta; o select espera. */
  resolvingCity?: boolean;
};

/**
 * Endereço da empresa, com UF e município em cascata.
 *
 * <p>
 * O backend não pré-carrega os mais de cinco mil municípios do país: cada
 * cliente usa um. Por isso o select de cidade traz o que já existe naquela UF e
 * oferece o cadastro na hora — sem essa saída, uma empresa numa cidade nova
 * ficaria impossível de cadastrar pela interface.
 */
export function AddressFields({
  states,
  fieldErrors,
  disabled,
  draft,
  patch,
  applyAddress,
  resolvingCity = false,
}: AddressFieldsProps) {
  const [cities, setCities] = useState<City[]>([]);
  const [loadingCities, startLoading] = useTransition();
  const [creatingCity, setCreatingCity] = useState(false);
  const [newCity, setNewCity] = useState({ name: "", ibgeCode: "" });
  const [saving, startSaving] = useTransition();
  const [lookingUp, startLookup] = useTransition();
  const toast = useToast();

  /**
   * Identifica a busca mais recente.
   *
   * Trocar de UF duas vezes seguidas deixa duas requisições no ar, e a primeira
   * pode responder por último — a lista exibida acabaria sendo a da UF antiga,
   * com a UF nova selecionada.
   */
  const requestId = useRef(0);
  const stateId = draft.stateId;

  // Buscar no servidor, e não no cliente, porque o token de acesso mora em
  // cookie httpOnly: o navegador não consegue chamar a API diretamente.
  //
  // O efeito só dispara a busca; limpar a lista é responsabilidade de quem troca
  // a UF, no próprio handler. Zerar aqui seria setState síncrono dentro do
  // efeito — uma renderização em cascata a cada montagem, por nada.
  useEffect(() => {
    if (!stateId) return;

    const current = ++requestId.current;

    startLoading(async () => {
      const loaded = await fetchCitiesAction(stateId);
      if (requestId.current === current) setCities(loaded);
    });
  }, [stateId]);

  const handleLookupCep = () => {
    startLookup(async () => {
      const result = await lookupCepAction(draft.cep);

      if (!result.ok) {
        toast.error(result.message);
        return;
      }

      applyAddress(result.data.address, (applied) => {
        if (applied.ok) toast.success(result.message);
        else toast.error(applied.message);
      });
    });
  };

  const handleCreateCity = () => {
    startSaving(async () => {
      const result = await createCityAction(stateId, newCity.name, newCity.ibgeCode);

      if (!result.ok || !result.city) {
        toast.error(result.message);
        return;
      }

      // Já deixa selecionada: quem cadastrou a cidade estava cadastrando a
      // empresa, e obrigar a procurá-la de novo na lista seria trabalho à toa.
      setCities((current) =>
        current.some((city) => city.id === result.city!.id) ? current : [...current, result.city!],
      );
      patch({ cityId: result.city.id, cityName: result.city.name });
      setCreatingCity(false);
      setNewCity({ name: "", ibgeCode: "" });
      toast.success(result.message);
    });
  };

  const cepReady = draft.cep.replace(/\D/g, "").length === 8;
  const busyCity = loadingCities || resolvingCity;

  return (
    <>
      <div className="flex items-end gap-2">
        <MaskedInput
          name="cep"
          mask="cep"
          label="CEP"
          placeholder="00000-000"
          inputMode="numeric"
          disabled={disabled}
          value={draft.cep}
          onValueChange={(digits) => patch({ cep: digits })}
          containerClassName="flex-1"
          hint="Preenche logradouro, município e UF."
          error={fieldErrors.cep}
          leadingIcon={<Icon name="building" />}
        />

        <Button
          type="button"
          variant="ghost"
          size="sm"
          className="mb-[26px]"
          disabled={disabled || lookingUp || !cepReady}
          onClick={handleLookupCep}
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

      <Select
        label="Estado"
        value={String(stateId || "")}
        disabled={disabled}
        onChange={(event) => {
          // A cidade anterior é de outra UF: manter a seleção — ou a lista —
          // gravaria um endereço incoerente.
          setCities([]);
          setCreatingCity(false);
          patch({ stateId: Number(event.target.value), cityId: 0, cityName: "" });
        }}
        options={[
          { label: "Selecione o estado", value: "" },
          ...states.map((state) => ({
            label: `${state.acronym} — ${state.name}`,
            value: String(state.id),
          })),
        ]}
      />

      {/*
        O id da cidade vai por campo oculto, e não pelo `name` do select.
        Select desabilitado não entra no `FormData`, e um `value` sem `option`
        correspondente — enquanto a lista da UF não chegou — é lido como vazio:
        nos dois casos a edição salvaria a empresa sem cidade.
      */}
      <input type="hidden" name="cityId" value={draft.cityId || ""} />

      <Select
        label="Cidade"
        value={String(draft.cityId || "")}
        disabled={disabled || !stateId || busyCity}
        error={fieldErrors.cityId}
        hint={
          !stateId
            ? "Escolha o estado primeiro."
            : busyCity
              ? "Carregando municípios..."
              : undefined
        }
        onChange={(event) => {
          const id = Number(event.target.value);
          patch({
            cityId: id,
            cityName: cities.find((city) => city.id === id)?.name ?? "",
          });
        }}
        options={[
          { label: stateId ? "Selecione a cidade" : "—", value: "" },
          ...cityOptions(cities, draft),
        ]}
      />

      {stateId > 0 && !creatingCity && (
        <button
          type="button"
          disabled={disabled}
          onClick={() => setCreatingCity(true)}
          className="self-start text-xs font-semibold text-accent-soft underline underline-offset-2"
        >
          A cidade não está na lista
        </button>
      )}

      {creatingCity && (
        <div className="flex flex-col gap-3 rounded-control border border-line p-3">
          <p className="text-xs text-ink-tertiary">
            Informe o município e o código do IBGE (7 dígitos). Ele passa a valer para todos os
            cadastros.
          </p>

          <Input
            label="Município"
            placeholder="Nome da cidade"
            value={newCity.name}
            disabled={saving}
            onChange={(event) => setNewCity((current) => ({ ...current, name: event.target.value }))}
          />

          <Input
            label="Código do IBGE"
            placeholder="0000000"
            inputMode="numeric"
            maxLength={7}
            value={newCity.ibgeCode}
            disabled={saving}
            onChange={(event) =>
              setNewCity((current) => ({
                ...current,
                ibgeCode: event.target.value.replace(/\D/g, "").slice(0, 7),
              }))
            }
          />

          <div className="flex gap-2">
            <Button
              type="button"
              size="sm"
              disabled={saving}
              onClick={handleCreateCity}
              trailingIcon={
                saving ? <Icon name="loader" className="h-4 w-4 animate-spin" /> : undefined
              }
            >
              {saving ? "Salvando..." : "Cadastrar cidade"}
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              disabled={saving}
              onClick={() => setCreatingCity(false)}
            >
              Cancelar
            </Button>
          </div>
        </div>
      )}

      <Input
        name="street"
        label="Logradouro"
        placeholder="Rua, avenida..."
        disabled={disabled}
        value={draft.street}
        onChange={(event) => patch({ street: event.target.value })}
        error={fieldErrors.street}
      />

      <div className="grid gap-4 sm:grid-cols-2">
        <Input
          name="number"
          label="Número"
          placeholder="123"
          maxLength={20}
          disabled={disabled}
          value={draft.number}
          onChange={(event) => patch({ number: event.target.value })}
          error={fieldErrors.number}
        />

        <Input
          name="complement"
          label="Complemento"
          placeholder="Sala, bloco (opcional)"
          disabled={disabled}
          value={draft.complement}
          onChange={(event) => patch({ complement: event.target.value })}
        />
      </div>
    </>
  );
}

/**
 * Opções do select, com a cidade selecionada garantida na lista.
 *
 * <p>
 * Ela pode não estar em `cities` em dois momentos: na abertura da edição, antes
 * de a lista da UF chegar, e logo depois de uma consulta de CEP resolver o
 * município. Sem a opção, o `<select>` mostraria o campo vazio e a pessoa
 * acharia que a cidade se perdeu.
 */
function cityOptions(cities: City[], draft: CompanyDraft) {
  const options = cities.map((city) => ({ label: city.name, value: String(city.id) }));

  if (draft.cityId && !cities.some((city) => city.id === draft.cityId)) {
    options.unshift({
      label: draft.cityName || "Cidade selecionada",
      value: String(draft.cityId),
    });
  }

  return options;
}
