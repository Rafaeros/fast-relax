/** Formato do `Page<T>` do Spring Data, como sai no JSON. */
export type SpringPage<T> = {
  content: T[];
  /** Índice da página atual, começando em 0. */
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
  empty: boolean;
};

/** Parâmetros de paginação aceitos pelo `Pageable`. */
export type PageParams = {
  page?: number;
  size?: number;
  /** Formato do Spring: "name,asc". */
  sort?: string;
};

/** Fatia normalizada para consumo na UI — o que a rolagem infinita precisa saber. */
export type PageSlice<T> = {
  rows: T[];
  page: number;
  hasMore: boolean;
  totalElements: number;
};

export function toPageSlice<T>(page: SpringPage<T>): PageSlice<T> {
  return {
    rows: page.content ?? [],
    page: page.number ?? 0,
    hasMore: !(page.last ?? true),
    totalElements: page.totalElements ?? 0,
  };
}

/** Fatia vazia — usada quando a API falha, para a tabela renderizar mesmo assim. */
export function emptyPageSlice<T>(): PageSlice<T> {
  return { rows: [], page: 0, hasMore: false, totalElements: 0 };
}
