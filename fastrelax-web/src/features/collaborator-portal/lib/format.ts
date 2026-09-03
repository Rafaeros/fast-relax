import type { CollaboratorSession } from "@/features/collaborator-portal/types/portal.types";

/**
 * Formatação das datas e horários do app.
 *
 * <p>
 * As datas da API vêm como `yyyy-MM-dd` sem fuso. Passar isso direto para
 * `new Date()` faz o JS interpretar como UTC e exibir o dia anterior à noite —
 * por isso as partes são desmembradas e remontadas em horário local.
 */

const WEEKDAYS = ["Domingo", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado"];
const MONTHS = ["jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"];

/** `yyyy-MM-dd` para `Date` local, sem o deslocamento de fuso do parse ISO. */
export function parseApiDate(value: string): Date {
  const [year, month, day] = value.split("-").map(Number);
  return new Date(year, month - 1, day);
}

/**
 * Instante local do fim agendado de uma sessão, combinando `sessionDate` e
 * `endTime`. É o alvo do cronômetro — não a hora de início real mais a
 * duração nominal, que empurraria o fim para depois do horário previsto
 * sempre que a sessão começasse atrasada (dentro da tolerância).
 */
export function sessionEndInstant(sessionDate: string, endTime: string): Date {
  const date = parseApiDate(sessionDate);
  const [hours, minutes] = endTime.split(":").map(Number);
  date.setHours(hours, minutes, 0, 0);
  return date;
}

/** `HH:mm:ss` para `HH:mm` — os segundos nunca interessam na tela. */
export function formatTime(value: string): string {
  return value.slice(0, 5);
}

export function formatTimeRange(start: string, end: string): string {
  return `${formatTime(start)} – ${formatTime(end)}`;
}

/** Ex.: "Segunda, 25 out". */
export function formatSessionDate(value: string): string {
  const date = parseApiDate(value);
  return `${WEEKDAYS[date.getDay()]}, ${date.getDate()} ${MONTHS[date.getMonth()]}`;
}

/** Ex.: "25 out" — para listas onde o dia da semana ocuparia espaço demais. */
export function formatShortDate(value: string): string {
  const date = parseApiDate(value);
  return `${date.getDate()} ${MONTHS[date.getMonth()]}`;
}

/** `Date` para `yyyy-MM-dd`, o formato que a API espera nos filtros. */
export function toApiDate(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${date.getFullYear()}-${month}-${day}`;
}

export function isToday(value: string): boolean {
  return value === toApiDate(new Date());
}

/** Ordena da mais recente para a mais antiga, por data e hora. */
export function byMostRecent(a: CollaboratorSession, b: CollaboratorSession): number {
  const dates = b.sessionDate.localeCompare(a.sessionDate);
  return dates !== 0 ? dates : b.startTime.localeCompare(a.startTime);
}
