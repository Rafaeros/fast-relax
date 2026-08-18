import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  /*
   * Sem `output: "export"`.
   *
   * O painel e o portal do colaborador dependem de servidor em runtime:
   * cookies httpOnly, Server Actions e o Route Handler das planilhas. Export
   * estático não tem nada disso — o build quebra na primeira rota dinâmica, e
   * mesmo passando, login e agenda parariam de funcionar.
   *
   * Para o Capacitor, o app empacota uma casca que aponta para este servidor
   * (`server.url` em capacitor.config.ts), em vez de embutir HTML estático.
   */
  images: {
    unoptimized: true,
  },

  /*
   * Acesso ao dev server por IP da rede (celular, outro micro, WebView do
   * Capacitor). Sem isto o Next recusa as requisições vindas de outra origem e
   * os chunks de /_next/static respondem erro — a página abre sem JavaScript,
   * o que faz máscara de CPF e navegação client pararem de funcionar.
   */
  allowedDevOrigins: ["10.48.0.189", "192.168.0.0/16", "10.0.0.0/8", "*.local"],
};

export default nextConfig;
