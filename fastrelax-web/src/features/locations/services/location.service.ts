import { apiFetch } from "@/lib/api/http";
import type { ApiResult } from "@/lib/api/api.types";
import { readAccessToken } from "@/features/authentication/services/session.service";
import type { City, SaveCityInput, State } from "@/features/locations/types/location.types";

/**
 * Domínio do IBGE (`/locations`), usado pelos selects do cadastro de empresa.
 *
 * <p>
 * Leitura é aberta a qualquer autenticado; criar município é da equipe da
 * plataforma. Os mais de cinco mil municípios não são pré-carregados no
 * backend — cada cliente usa um, e a lista inteira só encheria o banco.
 */

const RESOURCE = "/locations";

export async function listStates(): Promise<ApiResult<State[]>> {
  return apiFetch<State[]>(`${RESOURCE}/states`, {
    token: await readAccessToken(),
    // A lista das 27 UFs não muda; revalidar de hora em hora evita uma ida à
    // API a cada abertura do formulário.
    revalidate: 3600,
  });
}

export async function listCities(stateId: number): Promise<ApiResult<City[]>> {
  return apiFetch<City[]>(`${RESOURCE}/states/${stateId}/cities`, {
    token: await readAccessToken(),
  });
}

/**
 * Cadastra o município na hora de precisar dele. Reenviar um já cadastrado
 * devolve o existente em vez de erro — o cadastro de empresa não deve falhar
 * porque outro cliente da mesma cidade chegou antes.
 */
export async function createCity(
  stateId: number,
  input: SaveCityInput,
): Promise<ApiResult<City>> {
  return apiFetch<City>(`${RESOURCE}/states/${stateId}/cities`, {
    method: "POST",
    body: input,
    token: await readAccessToken(),
  });
}
