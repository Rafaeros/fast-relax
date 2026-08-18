import type { WebPushSubscription } from "@/features/notifications/types/notification.types";

/**
 * Conversa com as APIs de push do navegador.
 *
 * <p>
 * Tudo aqui roda no cliente e depende de contexto seguro: Service Worker e Push
 * API só existem em HTTPS ou em `localhost`. Num host interno servido por HTTP
 * puro (ex.: http://10.48.0.189) o navegador simplesmente não expõe essas APIs —
 * daí {@link isPushSupported} checar antes de qualquer tentativa.
 */

export const SERVICE_WORKER_PATH = "/sw.js";

export type PushSupport =
  | { supported: true }
  | { supported: false; reason: string };

export function checkPushSupport(): PushSupport {
  if (typeof window === "undefined") {
    return { supported: false, reason: "Indisponível no servidor." };
  }
  if (!window.isSecureContext) {
    return {
      supported: false,
      // A causa mais provável na rede da empresa, e a única acionável pelo TI.
      reason:
        "As notificações do navegador exigem HTTPS. Neste endereço em HTTP o navegador bloqueia o recurso.",
    };
  }
  if (!("serviceWorker" in navigator) || !("PushManager" in window)) {
    return { supported: false, reason: "Este navegador não suporta notificações push." };
  }
  if (!("Notification" in window)) {
    return { supported: false, reason: "Este navegador não suporta notificações." };
  }
  return { supported: true };
}

export function currentPermission(): NotificationPermission | null {
  if (typeof window === "undefined" || !("Notification" in window)) return null;
  return Notification.permission;
}

export async function registerServiceWorker(): Promise<ServiceWorkerRegistration> {
  const registration = await navigator.serviceWorker.register(SERVICE_WORKER_PATH);
  // `ready` só resolve quando há um worker ativo: inscrever antes disso falha
  // silenciosamente na primeira visita.
  await navigator.serviceWorker.ready;
  return registration;
}

/** Como este navegador está em relação ao push, do ponto de vista da tela. */
export type PushStatus =
  | { kind: "unsupported"; reason: string }
  | { kind: "off" }
  | { kind: "on" };

/**
 * Estado inicial do navegador, resolvido de uma vez.
 *
 * <p>
 * Assíncrono inclusive quando a resposta é imediata (navegador sem suporte):
 * assim quem chama tem um caminho só, e o resultado sempre chega por callback —
 * o que evita a cascata de renders de um `setState` síncrono dentro do efeito.
 */
export async function resolvePushStatus(): Promise<PushStatus> {
  const support = checkPushSupport();
  if (!support.supported) {
    return { kind: "unsupported", reason: support.reason };
  }

  try {
    // Uma inscrição existente é a única fonte confiável: a permissão pode estar
    // concedida sem haver inscrição (removida pelo navegador, perfil limpo).
    const subscription = await getExistingSubscription();
    return subscription ? { kind: "on" } : { kind: "off" };
  } catch {
    return { kind: "off" };
  }
}

export async function getExistingSubscription(): Promise<PushSubscription | null> {
  if (!checkPushSupport().supported) return null;

  const registration = await navigator.serviceWorker.getRegistration(SERVICE_WORKER_PATH);
  if (!registration) return null;

  return registration.pushManager.getSubscription();
}

/**
 * Pede permissão e cria a inscrição.
 *
 * @throws quando o usuário recusa — o navegador não volta a perguntar, e quem
 *         chama precisa dizer isso na tela em vez de tentar de novo
 */
export async function subscribeToPush(vapidPublicKey: string): Promise<WebPushSubscription> {
  const permission = await Notification.requestPermission();
  if (permission !== "granted") {
    throw new Error(
      "Permissão negada. Libere as notificações nas configurações do navegador para este site.",
    );
  }

  const registration = await registerServiceWorker();

  // Reaproveita a inscrição existente: assinar de novo com a mesma chave
  // devolveria a mesma coisa, e com chave diferente o navegador recusa.
  const existing = await registration.pushManager.getSubscription();
  const subscription =
    existing ??
    (await registration.pushManager.subscribe({
      // Obrigatório nos navegadores atuais: todo push recebido tem que gerar um
      // aviso visível. Push silencioso não é permitido.
      userVisibleOnly: true,
      applicationServerKey: urlBase64ToUint8Array(vapidPublicKey),
    }));

  return toPlainSubscription(subscription);
}

export async function unsubscribeFromPush(): Promise<string | null> {
  const subscription = await getExistingSubscription();
  if (!subscription) return null;

  const { endpoint } = subscription;
  await subscription.unsubscribe();
  return endpoint;
}

/**
 * `PushSubscription` é um objeto nativo: `JSON.stringify` funciona, mas o
 * spread não copia nada. Extrair os campos à mão é o que garante que a Server
 * Action receba dados serializáveis.
 */
export function toPlainSubscription(subscription: PushSubscription): WebPushSubscription {
  const json = subscription.toJSON();

  return {
    endpoint: subscription.endpoint,
    keys: {
      p256dh: json.keys?.p256dh ?? "",
      auth: json.keys?.auth ?? "",
    },
  };
}

/**
 * A chave VAPID viaja em base64url; `applicationServerKey` exige bytes.
 *
 * <p>
 * base64url troca `+/` por `-_` e dispensa o padding, então é preciso desfazer
 * as duas coisas antes de decodificar.
 */
function urlBase64ToUint8Array(base64String: string): Uint8Array<ArrayBuffer> {
  const padding = "=".repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, "+").replace(/_/g, "/");

  const raw = window.atob(base64);
  // Buffer explícito: `applicationServerKey` exige um ArrayBuffer comum, e o
  // construtor por tamanho é tipado como ArrayBufferLike (que admite
  // SharedArrayBuffer).
  const output = new Uint8Array(new ArrayBuffer(raw.length));
  for (let i = 0; i < raw.length; i += 1) {
    output[i] = raw.charCodeAt(i);
  }
  return output;
}
