/** Espelha `NotificationType` do backend. */
export type NotificationType =
  | "SESSION_SCHEDULED"
  | "SESSION_REMINDER"
  | "SESSION_STARTED"
  | "SESSION_FINISHED"
  | "SESSION_EXPIRED"
  | "SESSION_CANCELLED";

export type AppNotification = {
  id: number;
  type: NotificationType;
  /** Tipo em português, já pronto para exibição. */
  typeLabel: string;
  title: string;
  body: string;
  /** Carga livre do backend — hoje traz `sessionId`. */
  data: Record<string, unknown> | null;
  read: boolean;
  readAt: string | null;
  createdAt: string;
};

/** Plataformas aceitas em `POST /notifications/devices`. */
export type DevicePlatform = "ANDROID" | "IOS" | "WEB";

/**
 * Inscrição de Web Push no formato que o navegador produz — enviada ao backend
 * sem conversão, e é assim que ela fica gravada no JSONB.
 */
export type WebPushSubscription = {
  endpoint: string;
  keys: { p256dh: string; auth: string };
};

export type PortalActionResult = {
  ok: boolean;
  message: string;
};
