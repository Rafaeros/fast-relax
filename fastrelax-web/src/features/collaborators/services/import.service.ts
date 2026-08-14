import { apiFetch } from "@/lib/api/http";
import type { ApiResult } from "@/lib/api/api.types";
import { env } from "@/lib/env";
import { buildQuery } from "@/lib/api/query";
import { readAccessToken } from "@/features/authentication/services/session.service";
import type { ImportResult } from "@/features/collaborators/types/import.types";

/**
 * Importação e exportação de colaboradores (`/imports/collaborators`).
 * Restrito a ADMIN e RH.
 */

const RESOURCE = "/imports/collaborators";

/** Envia a planilha. Linhas com erro não abortam o arquivo — voltam no resultado. */
export async function importCollaborators(file: File): Promise<ApiResult<ImportResult>> {
  const body = new FormData();
  body.set("file", file);

  return apiFetch<ImportResult>(RESOURCE, {
    method: "POST",
    body,
    token: await readAccessToken(),
  });
}

export type SpreadsheetDownload =
  | { ok: true; content: ArrayBuffer; filename: string }
  | { ok: false; status: number; message: string };

/**
 * Baixa a planilha modelo ou a exportação.
 *
 * A resposta é binária, então não passa pelo `apiFetch`, que desempacota o
 * envelope JSON. Quem chama é um Route Handler: o token vive em cookie
 * httpOnly e o browser não conseguiria montar o `Authorization` sozinho.
 */
export async function downloadCollaboratorsSpreadsheet(
  kind: "template" | "export",
  options: { onlyActive?: boolean } = {},
): Promise<SpreadsheetDownload> {
  const path =
    kind === "template"
      ? `${RESOURCE}/template`
      : `${RESOURCE}/export${buildQuery({ onlyActive: options.onlyActive })}`;

  const filename = kind === "template" ? "modelo-colaboradores.xlsx" : "colaboradores.xlsx";

  let response: Response;

  try {
    response = await fetch(`${env.apiUrl}${path}`, {
      headers: { Authorization: `Bearer ${await readAccessToken()}` },
      cache: "no-store",
    });
  } catch {
    return { ok: false, status: 502, message: "Não foi possível falar com o servidor." };
  }

  if (!response.ok) {
    return {
      ok: false,
      status: response.status,
      message: "Não foi possível gerar a planilha.",
    };
  }

  return { ok: true, content: await response.arrayBuffer(), filename };
}
