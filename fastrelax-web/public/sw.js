/*
 * Service worker do FastRelax.
 *
 * Existe por um motivo só: receber push. O navegador só entrega notificação a
 * um service worker, porque ele roda mesmo com a aba fechada — que é justamente
 * quando o lembrete "sua massagem começa em 10 minutos" importa.
 *
 * Não faz cache de nada de propósito: o app depende de dados de sessão que
 * mudam a cada minuto, e um cache aqui serviria conteúdo velho sem ganho real
 * numa rede interna.
 */

const FALLBACK = {
  title: "FastRelax",
  body: "Você tem um aviso novo.",
  url: "/colaborador",
};

self.addEventListener("install", () => {
  // Assume o controle sem esperar a aba antiga fechar: sem isto, a primeira
  // inscrição só passaria a receber push depois de um recarregamento.
  self.skipWaiting();
});

self.addEventListener("activate", (event) => {
  event.waitUntil(self.clients.claim());
});

self.addEventListener("push", (event) => {
  let payload = FALLBACK;

  try {
    if (event.data) {
      payload = { ...FALLBACK, ...event.data.json() };
    }
  } catch {
    // Corpo ilegível não pode virar silêncio: o navegador exige que todo push
    // recebido mostre algo visível, sob pena de revogar a permissão do site.
  }

  event.waitUntil(
    self.registration.showNotification(payload.title, {
      body: payload.body,
      // Marca reduzida: é o que fica legível no tamanho de um ícone de aviso.
      icon: "/brand/logo-physical-mark.png",
      badge: "/brand/logo-physical-mark.png",
      // Avisos do mesmo assunto se substituem em vez de empilhar.
      tag: payload.tag || "fastrelax",
      renotify: true,
      data: { url: payload.url || FALLBACK.url, ...(payload.data || {}) },
    }),
  );
});

self.addEventListener("notificationclick", (event) => {
  event.notification.close();

  const target = (event.notification.data && event.notification.data.url) || FALLBACK.url;

  event.waitUntil(
    self.clients.matchAll({ type: "window", includeUncontrolled: true }).then((clients) => {
      // Reaproveita uma aba já aberta do app: abrir outra deixaria o
      // colaborador com duas janelas do mesmo portal.
      for (const client of clients) {
        if (client.url.includes("/colaborador") && "focus" in client) {
          client.navigate(target);
          return client.focus();
        }
      }
      return self.clients.openWindow(target);
    }),
  );
});
