"use client";

import { useTransition } from "react";
import { Button, Card, Icon, useToast } from "@/components/ui";
import type { IconName } from "@/components/ui/Icon";
import { cn } from "@/lib/cn";
import {
  markAllNotificationsReadAction,
  markNotificationReadAction,
} from "@/features/notifications/actions/notification.actions";
import type {
  AppNotification,
  NotificationType,
} from "@/features/notifications/types/notification.types";

export type NotificationListProps = {
  notifications: AppNotification[];
  unreadCount: number;
};

/** Ícone por tipo — o mesmo vocabulário visual do resto do app. */
const TYPE_ICON: Record<NotificationType, IconName> = {
  SESSION_SCHEDULED: "calendar",
  SESSION_REMINDER: "bell",
  SESSION_STARTED: "play",
  SESSION_FINISHED: "check",
  SESSION_EXPIRED: "clock",
  SESSION_CANCELLED: "close",
};

export function NotificationList({ notifications, unreadCount }: NotificationListProps) {
  const [pending, startTransition] = useTransition();
  const { error } = useToast();

  const markOne = (id: number) => {
    startTransition(async () => {
      const result = await markNotificationReadAction(id);
      if (!result.ok) error(result.message);
    });
  };

  const markAll = () => {
    startTransition(async () => {
      const result = await markAllNotificationsReadAction();
      if (!result.ok) error(result.message);
    });
  };

  if (notifications.length === 0) {
    return (
      <Card padding="lg" className="flex flex-col items-center gap-3 text-center">
        <span className="flex h-12 w-12 items-center justify-center rounded-full bg-surface-hover">
          <Icon name="bell" className="h-5 w-5 text-ink-muted" />
        </span>
        <div>
          <p className="text-sm font-semibold text-ink-primary">Nenhuma notificação</p>
          <p className="mt-1 text-xs text-ink-tertiary">
            Avisos sobre suas massagens aparecem aqui.
          </p>
        </div>
      </Card>
    );
  }

  return (
    <div className="flex flex-col gap-3">
      {unreadCount > 0 && (
        <div className="flex items-center justify-between gap-3">
          <span className="text-xs text-ink-tertiary">
            {unreadCount} não {unreadCount === 1 ? "lida" : "lidas"}
          </span>
          <Button variant="ghost" size="sm" onClick={markAll} disabled={pending}>
            Marcar todas como lidas
          </Button>
        </div>
      )}

      <Card padding="none" className="flex flex-col">
        <ul className="flex flex-col divide-y divide-line">
          {notifications.map((notification) => (
            <li key={notification.id}>
              {/*
                A linha inteira marca como lida: em tela de celular um botão
                dedicado por item roubaria espaço do texto, que é o conteúdo.
              */}
              <button
                type="button"
                onClick={() => !notification.read && markOne(notification.id)}
                disabled={pending || notification.read}
                className={cn(
                  "flex w-full items-start gap-3 p-4 text-left",
                  "focus-visible:outline-none focus-visible:shadow-focus",
                  notification.read ? "cursor-default" : "bg-accent/5 hover:bg-accent/10",
                )}
              >
                <span
                  className={cn(
                    "mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-full",
                    notification.read ? "bg-surface-hover" : "bg-accent/20",
                  )}
                >
                  <Icon
                    name={TYPE_ICON[notification.type] ?? "bell"}
                    className={cn(
                      "h-4 w-4",
                      notification.read ? "text-ink-muted" : "text-accent-soft",
                    )}
                  />
                </span>

                <span className="flex min-w-0 flex-1 flex-col gap-0.5">
                  <span className="flex items-center gap-2">
                    <span
                      className={cn(
                        "truncate text-sm",
                        notification.read
                          ? "text-ink-secondary"
                          : "font-semibold text-ink-primary",
                      )}
                    >
                      {notification.title}
                    </span>
                    {!notification.read && (
                      <span
                        aria-label="Não lida"
                        className="h-2 w-2 shrink-0 rounded-full bg-accent"
                      />
                    )}
                  </span>
                  <span className="text-xs text-ink-tertiary">{notification.body}</span>
                  <span className="text-[0.6875rem] text-ink-muted">
                    {formatRelative(notification.createdAt)}
                  </span>
                </span>
              </button>
            </li>
          ))}
        </ul>
      </Card>
    </div>
  );
}

/**
 * "há 5 min" diz mais que a data cheia numa lista de avisos recentes; passada
 * uma semana, a data volta a ser mais informativa que "há 9 dias".
 */
function formatRelative(iso: string): string {
  const date = new Date(iso);
  const minutes = Math.round((Date.now() - date.getTime()) / 60000);

  if (minutes < 1) return "agora";
  if (minutes < 60) return `há ${minutes} min`;

  const hours = Math.round(minutes / 60);
  if (hours < 24) return `há ${hours} h`;

  const days = Math.round(hours / 24);
  if (days < 7) return `há ${days} ${days === 1 ? "dia" : "dias"}`;

  return date.toLocaleDateString("pt-BR", { day: "2-digit", month: "2-digit", year: "numeric" });
}
