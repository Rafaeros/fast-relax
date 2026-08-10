const path = require('path');
const os = require('os');

const isWindows = os.platform() === 'win32';

module.exports = {
  apps: [
    {
      name: 'fastrelax-api',

      cwd: __dirname,

      script: 'java',

      args: [
        '-jar',
        'target/fastrelax-api-1.0.0.jar',
      ],

      interpreter: 'none',

      env: {
        SPRING_PROFILES_ACTIVE: 'prod',
      },

      instances: 1,
      exec_mode: 'fork',

      autorestart: true,
      watch: false,

      max_memory_restart: '500M',

      time: true,

      error_file: path.join(__dirname, 'logs', 'error.log'),
      out_file: path.join(__dirname, 'logs', 'out.log'),
      log_date_format: 'YYYY-MM-DD HH:mm:ss',

      kill_timeout: 5000,
      listen_timeout: 10000,

      windowsHide: isWindows,
    },
  ],
};