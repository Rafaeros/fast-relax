"use client";

import { useCallback, useState, useTransition } from "react";
import { onlyDigits } from "@/lib/format";
import { createCityAction } from "@/features/locations/actions/location.actions";
import type { State } from "@/features/locations/types/location.types";
import type { Company } from "@/features/companies/types/company.types";
import type { LookupAddress } from "@/features/lookup/types/lookup.types";

/**
 * Campos do cadastro de empresa que uma consulta externa pode preencher.
 *
 * <p>
 * Estes deixaram de ser campos soltos do formulário porque o CNPJ preenche o
 * endereço e o CEP preenche o logradouro: com `defaultValue`, um input não
 * aceita valor vindo de fora depois de montado — a consulta responderia e a
 * tela continuaria em branco.
 */
export type CompanyDraft = {
  cnpj: string;
  name: string;
  email: string;
  phone: string;
  stateId: number;
  cityId: number;
  /**
   * Nome do município escolhido.
   *
   * Guardado junto com o id porque a lista de municípios da UF chega por
   * requisição: até ela responder, é este rótulo que mantém a cidade visível
   * no select em vez de o campo aparecer vazio.
   */
  cityName: string;
  cep: string;
  street: string;
  number: string;
  complement: string;
};

export type ApplyAddressResult = { ok: boolean; message: string };

export type UseCompanyDraftOptions = {
  states: State[];
  /** Registro em edição — ausente no cadastro. */
  company?: Company;
};

/**
 * Estado compartilhado entre os campos da empresa e os do endereço.
 *
 * <p>
 * Vive aqui, e não em cada bloco, porque a consulta de CNPJ atravessa os dois:
 * ela traz razão social e endereço na mesma resposta.
 */
export function useCompanyDraft({ states, company }: UseCompanyDraftOptions) {
  const [draft, setDraft] = useState<CompanyDraft>(() => initialDraft(states, company));
  const [resolvingCity, startResolving] = useTransition();

  const patch = useCallback((changes: Partial<CompanyDraft>) => {
    setDraft((current) => ({ ...current, ...changes }));
  }, []);

  /**
   * Aplica um endereço vindo de consulta externa.
   *
   * <p>
   * Campo vazio na resposta não apaga o que já está na tela: o CEP não conhece
   * o número, e sobrescrever com vazio faria a consulta destruir o que a pessoa
   * acabou de digitar.
   */
  const applyAddress = useCallback(
    (address: LookupAddress, onDone?: (result: ApplyAddressResult) => void) => {
      patch(
        keepFilled({
          cep: address.cep,
          street: address.street,
          number: address.number,
          complement: address.complement,
        }),
      );

      const state = states.find((entry) => entry.acronym === address.stateAcronym);

      if (!state) {
        onDone?.({
          ok: false,
          message: address.stateAcronym
            ? `Estado ${address.stateAcronym} não está cadastrado. Escolha a cidade manualmente.`
            : "A consulta não trouxe o estado. Escolha a cidade manualmente.",
        });
        return;
      }

      patch({ stateId: state.id });

      const ibgeCode = onlyDigits(address.ibgeCode);

      if (ibgeCode.length !== 7 || !address.cityName) {
        onDone?.({
          ok: false,
          message: "A consulta não identificou o município. Escolha a cidade manualmente.",
        });
        return;
      }

      // O município do endereço consultado pode não existir no nosso cadastro —
      // os mais de cinco mil do país não são pré-carregados. Cadastrar na hora
      // devolve o existente quando já houver, então isto serve aos dois casos.
      startResolving(async () => {
        const result = await createCityAction(state.id, address.cityName, ibgeCode);

        if (!result.ok || !result.city) {
          onDone?.({ ok: false, message: result.message });
          return;
        }

        patch({ cityId: result.city.id, cityName: result.city.name });
        onDone?.({ ok: true, message: "" });
      });
    },
    [patch, states],
  );

  return { draft, patch, applyAddress, resolvingCity };
}

/** Descarta as chaves vazias para o patch não apagar o que já está preenchido. */
function keepFilled(values: Record<string, string>): Partial<CompanyDraft> {
  return Object.fromEntries(Object.entries(values).filter(([, value]) => value !== ""));
}

function initialDraft(states: State[], company?: Company): CompanyDraft {
  const stateId = states.find((state) => state.acronym === company?.stateAcronym)?.id ?? 0;

  return {
    cnpj: company?.cnpj ?? "",
    name: company?.name ?? "",
    email: company?.email ?? "",
    phone: company?.phone ?? "",
    stateId,
    cityId: company?.cityId ?? 0,
    cityName: company?.cityName ?? "",
    cep: company?.cep ?? "",
    street: company?.street ?? "",
    number: company?.number ?? "",
    complement: company?.complement ?? "",
  };
}
