import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { Alert } from "@/components/ui";
import { getCurrentCollaborator } from "@/features/collaborator-portal/services/portal.service";
import { NotificationList } from "@/features/notifications/components/NotificationList";
import { listMyNotifications } from "@/features/notifications/services/notification.service";

export const metadata: Metadata = {
  title: "Notificações — physical",
};

/**
 * Central de notificações.
 *
 * <p>
 * Existe porque push é entrega best-effort: celular desligado, permissão
 * revogada ou navegador fechado fazem o aviso se perder sem deixar rastro. Aqui
 * ele está gravado, e a lista continua completa mesmo para quem nunca ativou as
 * notificações do navegador.
 */
export default async function CollaboratorNotificationsPage() {
  const collaborator = await getCurrentCollaborator();
  if (!collaborator) redirect("/colaborador/entrar");

  const result = await listMyNotifications();
  const notifications = result.ok ? result.data.content : [];
  const unreadCount = notifications.filter((notification) => !notification.read).length;

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-1">
        <h1 className="font-display text-2xl text-ink-primary">Notificações</h1>
        <p className="text-sm text-ink-secondary">
          Avisos sobre suas massagens, mesmo os que você não recebeu no aparelho.
        </p>
      </div>

      {!result.ok && (
        <Alert tone="error" title="Não foi possível carregar suas notificações">
          {result.message}
        </Alert>
      )}

      <NotificationList notifications={notifications} unreadCount={unreadCount} />
    </div>
  );
}
