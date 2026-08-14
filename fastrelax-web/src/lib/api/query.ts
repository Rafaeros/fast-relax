/**
 * Monta a query string ignorando valores vazios — filtro não preenchido não
 * pode virar `?name=` na URL, senão o backend filtra por string vazia.
 */
export function buildQuery(params: Record<string, string | number | boolean | undefined | null>): string {
  const search = new URLSearchParams();

  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === null || value === "") continue;
    search.set(key, String(value));
  }

  const query = search.toString();
  return query ? `?${query}` : "";
}
