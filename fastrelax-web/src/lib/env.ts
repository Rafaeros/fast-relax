/**
 * Configuração de ambiente lida apenas no servidor.
 * A URL da API nunca é exposta ao browser: todo acesso passa por Server Actions
 * ou Server Components, então o token também nunca chega ao bundle do cliente.
 */
export const env = {
  apiUrl: process.env.FASTRELAX_API_URL ?? "http://localhost:8090/api/v1",
  isProduction: process.env.NODE_ENV === "production",
} as const;
