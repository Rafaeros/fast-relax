import { Badge, Card, DetailList, Icon } from "@/components/ui";
import type { DetailItem } from "@/components/ui";
import { formatCpf, formatPhone } from "@/lib/format";
import { formatTimeRange } from "@/features/collaborator-portal/lib/format";
import type {
  AllowedWindow,
  CollaboratorProfile,
} from "@/features/collaborator-portal/types/portal.types";

export type ProfileViewProps = {
  profile: CollaboratorProfile;
  windows: AllowedWindow[];
};

/** Perfil do colaborador e as janelas em que ele pode agendar. */
export function ProfileView({ profile, windows }: ProfileViewProps) {
  const items: DetailItem[] = [
    { label: "Nome", value: profile.name, full: true },
    { label: "CPF", value: formatCpf(profile.cpf) },
    { label: "Telefone", value: formatPhone(profile.phoneNumber) },
    { label: "Departamento", value: profile.departmentName ?? "—" },
    {
      label: "Situação",
      value: (
        <Badge tone={profile.active ? "success" : "neutral"}>
          {profile.active ? "Ativo" : "Inativo"}
        </Badge>
      ),
    },
  ];

  return (
    <div className="flex flex-col gap-4">
      <Card padding="lg" className="flex flex-col gap-5">
        <div className="flex items-center gap-3">
          {/* Iniciais no lugar de foto: o cadastro não tem imagem, e um
              placeholder genérico diria menos que as letras do próprio nome. */}
          <span className="flex h-14 w-14 shrink-0 items-center justify-center rounded-full bg-accent/15 text-lg font-semibold text-accent-soft">
            {initials(profile.name)}
          </span>
          <div className="min-w-0">
            <p className="truncate font-display text-xl text-ink-primary">{profile.name}</p>
            <p className="truncate text-sm text-ink-tertiary">
              {profile.departmentName ?? "Sem departamento"}
            </p>
          </div>
        </div>

        <DetailList items={items} />
      </Card>

      <Card padding="none" className="flex flex-col">
        <div className="flex flex-col gap-1 border-b border-line p-4">
          <h2 className="text-sm font-semibold text-ink-primary">Seus horários permitidos</h2>
          <p className="text-xs text-ink-tertiary">
            Faixa em que você pode marcar massagem. Definida pelo RH.
          </p>
        </div>

        {windows.length === 0 ? (
          <p className="p-8 text-center text-sm text-ink-tertiary">
            Nenhum horário configurado. Procure o RH para liberar seus agendamentos.
          </p>
        ) : (
          <ul className="flex flex-col divide-y divide-line">
            {windows.map((window) => (
              <li key={window.id} className="flex items-center gap-3 p-4">
                <Icon name="clock" className="h-4 w-4 shrink-0 text-ink-muted" />
                <span className="flex-1 text-sm text-ink-primary">{window.dayOfWeekLabel}</span>
                <span className="text-sm tabular-nums text-ink-secondary">
                  {formatTimeRange(window.allowedStartTime, window.allowedEndTime)}
                </span>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  );
}

/** Primeira e última palavra do nome — "Maria dos Santos Silva" vira "MS". */
function initials(name: string): string {
  const parts = name.trim().split(/\s+/);
  if (parts.length === 0) return "?";
  const first = parts[0]?.[0] ?? "";
  const last = parts.length > 1 ? (parts[parts.length - 1]?.[0] ?? "") : "";
  return (first + last).toUpperCase();
}
