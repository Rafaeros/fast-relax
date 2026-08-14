"use client";

import { useEffect, useState } from "react";

/**
 * Atrasa a propagação do valor até ele parar de mudar por `delay` ms.
 * Evita uma requisição por tecla digitada na busca.
 */
export function useDebouncedValue<T>(value: T, delay = 400): T {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(timer);
  }, [value, delay]);

  return debounced;
}
