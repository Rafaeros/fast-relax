import type { CapacitorConfig } from '@capacitor/cli';

/**
 * O app do colaborador é uma casca sobre o servidor Next, não um pacote de
 * HTML estático.
 *
 * Login, agenda e início de sessão dependem de cookie httpOnly e Server
 * Actions — nada disso existe em `output: "export"`. Apontando `server.url`
 * para o servidor, o WebView usa exatamente o mesmo app do navegador, com uma
ww * base de código só.
 *
 * `cleartext` libera HTTP: em rede interna, sem TLS, o Android bloquearia a
 * conexão por padrão. Ao publicar com domínio e HTTPS, troque a URL e remova
 * esta linha.
 */
const SERVER_URL = process.env.CAPACITOR_SERVER_URL ?? 'http://10.48.0.189';

const config: CapacitorConfig = {
  appId: 'br.fastrelax.com',
  appName: 'FastRelax',
  // Exigido pelo CLI mesmo com `server.url`; serve de fallback offline.
  webDir: 'public',
  server: {
    url: SERVER_URL,
    cleartext: true,
    androidScheme: 'http',
  },
};

export default config;
