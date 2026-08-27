/** Espelha `StateResponseDTO` do fastrelax-api. */
export type State = {
  id: number;
  name: string;
  acronym: string;
  ibgeCode: string;
};

/** Espelha `CityResponseDTO`. */
export type City = {
  id: number;
  stateId: number | null;
  stateAcronym: string | null;
  name: string;
  ibgeCode: string;
};

/** Espelha `SaveCityRequestDTO`. */
export type SaveCityInput = {
  name: string;
  /** Código do IBGE do município: 7 dígitos, e é ele que garante a unicidade. */
  ibgeCode: string;
};
