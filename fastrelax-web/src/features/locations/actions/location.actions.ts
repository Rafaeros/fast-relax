"use server";

import { createCity, listCities } from "@/features/locations/services/location.service";
import type { City } from "@/features/locations/types/location.types";

/**
 * Municípios de uma UF, para o select do endereço.
 *
 * <p>
 * Server Action em vez de rota: o formulário é um componente cliente e não tem
 * o token — ele mora em cookie httpOnly, que só o servidor lê.
 */
export async function fetchCitiesAction(stateId: number): Promise<City[]> {
  const result = await listCities(stateId);
  // Falha vira lista vazia: o select fica sem opções e o formulário mostra o
  // erro do campo, em vez de a tela inteira quebrar por causa do endereço.
  return result.ok ? result.data : [];
}

export type CreateCityResult = { ok: boolean; message: string; city?: City };

/** Cadastra o município que faltava, sem sair do formulário da empresa. */
export async function createCityAction(
  stateId: number,
  name: string,
  ibgeCode: string,
): Promise<CreateCityResult> {
  const trimmedName = name.trim();
  const digits = ibgeCode.replace(/\D/g, "");

  if (trimmedName.length < 2) {
    return { ok: false, message: "Informe o nome do município." };
  }
  if (digits.length !== 7) {
    return { ok: false, message: "O código do IBGE do município tem 7 dígitos." };
  }

  const result = await createCity(stateId, { name: trimmedName, ibgeCode: digits });

  return result.ok
    ? { ok: true, message: result.message, city: result.data }
    : { ok: false, message: result.message };
}
