type ClassValue = string | number | bigint | null | undefined | false | ClassValue[];

/**
 * Concatena classes condicionais sem dependências externas.
 * Uso: cn("base", isActive && "ativa", props.className)
 */
export function cn(...values: ClassValue[]): string {
  const out: string[] = [];

  for (const value of values) {
    if (!value) continue;
    if (Array.isArray(value)) {
      const nested = cn(...value);
      if (nested) out.push(nested);
      continue;
    }
    out.push(String(value));
  }

  return out.join(" ");
}
