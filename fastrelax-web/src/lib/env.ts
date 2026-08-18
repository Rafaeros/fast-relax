/**
 * Configuração de ambiente lida apenas no servidor.
 * A URL da API nunca é exposta ao browser: todo acesso passa por Server Actions
 * ou Server Components, então o token também nunca chega ao bundle do cliente.
 */
const isProduction = process.env.NODE_ENV === "production";

export const env = {
  apiUrl: process.env.FASTRELAX_API_URL ?? "http://localhost:8090/api/v1",
  isProduction,

  /**
   * Marca os cookies de sessão como `secure`.
   *
   * Cookie `secure` só é aceito em HTTPS. Servindo por IP na rede interna
   * (`http://10.48.0.189`), o browser descarta o cookie em silêncio: o login
   * responde 200 e a navegação seguinte volta para a tela de entrada.
   *
   * Padrão: ligado em produção. Ponha `SESSION_COOKIE_SECURE=false` no ambiente
   * enquanto o acesso for HTTP puro, e remova assim que houver TLS.
   */
  secureCookies: process.env.SESSION_COOKIE_SECURE
    ? process.env.SESSION_COOKIE_SECURE === "true"
    : isProduction,
} as const;
