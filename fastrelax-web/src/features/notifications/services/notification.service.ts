import { apiFetch } from "@/lib/api/http";
import type { ApiResult } from "@/lib/api/api.types";
import type { SpringPage } from "@/lib/api/pagination.types";
import { buildQuery } from "@/lib/api/query";
import { readAccessToken } from "@/features/authentication/services/session.service";
import type {
  AppNotification,
  DevicePlatform,
  WebPushSubscription,
} from "@/features/notifications/types/notification.types";

/**
 * Acesso à API de notificações.
 *
 * <p>
 * Roda só no servidor: o token vive em cookie httpOnly, inacessível ao
 * JavaScript da página.
 */

export const NOTIFICATIONS_PAGE_SIZE = 20;

export async function listMyNotifications(
  page = 0,
  size = NOTIFICATIONS_PAGE_SIZE,
): Promise<ApiResult<SpringPage<AppNotification>>> {
  return apiFetch<SpringPage<AppNotification>>(
    `/notifications${buildQuery({ page, size })}`,
    { token: await readAccessToken() },
  );
}

export async function getUnreadCount(): Promise<ApiResult<{ count: number }>> {
  return apiFetch<{ count: number }>("/notifications/unread-count", {
    token: await readAccessToken(),
  });
}

export async function markNotificationRead(id: number): Promise<ApiResult<AppNotification>> {
  return apiFetch<AppNotification>(`/notifications/${id}/read`, {
    method: "PATCH",
    token: await readAccessToken(),
  });
}

export async function markAllNotificationsRead(): Promise<ApiResult<{ updated: number }>> {
  return apiFetch<{ updated: number }>("/notifications/read-all", {
    method: "PATCH",
    token: await readAccessToken(),
  });
}

/**
 * Chave pública VAPID.
 *
 * <p>
 * Vem da API em vez de ficar embutida no bundle: trocar o par de chaves passa a
 * ser mudança de configuração do servidor, sem rebuild do frontend.
 */
export async function getVapidPublicKey(): Promise<ApiResult<{ publicKey: string }>> {
  return apiFetch<{ publicKey: string }>("/notifications/devices/vapid-public-key", {
    token: await readAccessToken(),
  });
}

/** Um corpo para as duas tecnologias: token (FCM) ou inscrição (Web Push). */
export async function registerDevice(input: {
  platform: DevicePlatform;
  token?: string;
  pushSubscription?: WebPushSubscription;
}): Promise<ApiResult<unknown>> {
  return apiFetch<unknown>("/notifications/devices", {
    method: "POST",
    body: input,
    token: await readAccessToken(),
  });
}

export async function unregisterSubscription(endpoint: string): Promise<ApiResult<unknown>> {
  return apiFetch<unknown>(
    `/notifications/devices/subscription${buildQuery({ endpoint })}`,
    { method: "DELETE", token: await readAccessToken() },
  );
}
