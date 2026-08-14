const path = require('path');
const os = require('os');

const isWindows = os.platform() === 'win32';

/**
 * O Next é iniciado pelo binário local em node_modules em vez de `npm start`.
 *
 * Chamar o gerenciador de pacotes criaria um processo intermediário: o PM2
 * monitoraria o npm/pnpm, não o servidor, e um restart deixaria o Next órfão
 * segurando a porta. Apontando direto para o script, o PM2 controla o processo
 * que de fato atende as requisições.
 *
 * O caminho muda entre plataformas — no Windows o executável é `next.cmd` —,
 * por isso a resolução condicional.
 */
const nextBin = path.join(
  __dirname,
  'node_modules',
  'next',
  'dist',
  'bin',
  'next',
);

module.exports = {
  apps: [
    {
      name: 'fastrelax-web',

      cwd: __dirname,

      // Executa o script do Next com o próprio Node: funciona igual nos dois
      // sistemas, sem depender de shell nem de PATH.
      script: nextBin,
      // Mesma porta do `pnpm dev`, para o painel ser alcançado sem porta na URL.
      args: ['start', '-H', '0.0.0.0', '-p', '80'],
      interpreter: 'node',

      // FASTRELAX_API_URL fica de fora de propósito: variável definida aqui
      // entra em process.env antes do Next carregar o .env, e o Next não
      // sobrescreve o que já existe. Declarar nos dois lugares faria o .env ser
      // silenciosamente ignorado. O endereço da API mora só no .env.
      env: {
        NODE_ENV: 'production',
        PORT: '80',
      },

      // Fork com uma instância: o Next já usa os núcleos por conta própria, e
      // cluster exigiria cache compartilhado entre os processos.
      instances: 1,
      exec_mode: 'fork',

      autorestart: true,
      watch: false,

      max_memory_restart: '500M',

      time: true,

      error_file: path.join(__dirname, 'logs', 'error.log'),
      out_file: path.join(__dirname, 'logs', 'out.log'),
      log_date_format: 'YYYY-MM-DD HH:mm:ss',

      // Margem para o Next encerrar as conexões em andamento antes do SIGKILL.
      kill_timeout: 5000,
      listen_timeout: 10000,

      windowsHide: isWindows,
    },
  ],
};
