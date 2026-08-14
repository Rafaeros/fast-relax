import { BaseApiResponse, ApiError } from "@/types/api.types";

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:3333";

export async function apiFetch<T>(endpoint: string, options?: RequestInit): Promise<T> {
  const url = `${BASE_URL}${endpoint}`;

  const response = await fetch(url, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...options?.headers,
    },
  });

  const result: BaseApiResponse<T> = await response.json();

  if (!response.ok || result.status !== "success") {
    throw new ApiError(
      result.message || "Ocorreu um erro inesperado na requisição.", 
      result.status || "error"
    );
  }

  return result.data;
}