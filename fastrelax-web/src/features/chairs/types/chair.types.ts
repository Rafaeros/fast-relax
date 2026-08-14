import type { PageParams } from "@/lib/api/pagination.types";

/** Espelha `ChairResponseDTO` do fastrelax-api. */
export type Chair = {
  id: number;
  name: string;
  macAddress: string;
  ipAddress: string | null;
  port: number;
  active: boolean;
  /** Derivado do último heartbeat no backend, não persistido. */
  online: boolean;
  lastSeenAt: string | null;
  createdAt: string;
};

/** Espelha `ChairFilterDTO`. */
export type ChairFilter = {
  name?: string;
  active?: boolean;
  online?: boolean;
};

export type ListChairsParams = ChairFilter & PageParams;

/**
 * Espelha `SaveChairRequestDTO`, usado no cadastro e na edição.
 * `ipAddress` e `port` são opcionais: o heartbeat do ESP32 preenche assim que o
 * dispositivo se anuncia.
 */
export type SaveChairInput = {
  name: string;
  macAddress: string;
  ipAddress?: string;
  port?: number;
};

export type ChairFieldErrors = Partial<
  Record<"name" | "macAddress" | "ipAddress" | "port", string>
>;

/** Estado dos formulários de cadastro e edição. */
export type ChairFormState = {
  status: "idle" | "success" | "error";
  message?: string;
  fieldErrors?: ChairFieldErrors;
};

export const CHAIR_INITIAL_STATE: ChairFormState = { status: "idle" };
