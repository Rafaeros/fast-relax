"use server";

import { revalidatePath } from "next/cache";
import {
  getVapidPublicKey,
  markAllNotificationsRead,
  markNotificationRead,
  registerDevice,
  unregisterSubscription,
} from "@/features/notifications/services/notification.service";
import type {
  PortalActionResult,
  WebPushSubscription,
} from "@/features/notifications/types/notification.types";

const NOTIFICATIONS = "/colaborador/notificacoes";

export async function markNotificationReadAction(id: number): Promise<PortalActionResult> {
  const result = await markNotificationRead(id);
  if (result.ok) revalidatePath(NOTIFICATIONS);

  return { ok: result.ok, message: result.message };
}

export async function markAllNotificationsReadAction(): Promise<PortalActionResult> {
  const result = await markAllNotificationsRead();
  if (result.ok) revalidatePath(NOTIFICATIONS);

  return { ok: result.ok, message: result.message };
}

/**
 * Chave pública VAPID para o navegador se inscrever.
 *
 * <p>
 * Existe como action porque a inscrição acontece no cliente, onde não há como
 * ler o cookie httpOnly que autentica a chamada à API.
 */
export async function getVapidPublicKeyAction(): Promise<string | null> {
  const result = await getVapidPublicKey();
  if (!result.ok || !result.data.publicKey) return null;

  return result.data.publicKey;
}

/** Guarda a inscrição do navegador como um destino de push do colaborador. */
export async function registerWebPushAction(
  subscription: WebPushSubscription,
): Promise<PortalActionResult> {
  const result = await registerDevice({ platform: "WEB", pushSubscription: subscription });
  return { ok: result.ok, message: result.message };
}

export async function unregisterWebPushAction(endpoint: string): Promise<PortalActionResult> {
  const result = await unregisterSubscription(endpoint);
  return { ok: result.ok, message: result.message };
}

/**
 * Registro do token do FCM, usado pelo app empacotado com Capacitor.
 *
 * <p>
 * O plugin de push nativo devolve o token no cliente; daqui ele segue para a
 * API com o mesmo cookie de sessão da web.
 */
export async function registerFcmTokenAction(
  token: string,
  platform: "ANDROID" | "IOS",
): Promise<PortalActionResult> {
  const result = await registerDevice({ platform, token });
  return { ok: result.ok, message: result.message };
}
