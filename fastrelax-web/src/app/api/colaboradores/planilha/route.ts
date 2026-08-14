import type { NextRequest } from "next/server";
import { downloadCollaboratorsSpreadsheet } from "@/features/collaborators/services/import.service";

const XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

/**
 * Ponte de download das planilhas de colaboradores.
 *
 * O arquivo vem da API autenticado por Bearer, mas o token mora em cookie
 * httpOnly — inacessível ao JavaScript da página. Este handler roda no
 * servidor, monta o cabeçalho e devolve os bytes, então um link comum basta
 * para o usuário baixar.
 *
 * `?tipo=modelo` traz a planilha em branco; sem ele, exporta os colaboradores.
 * `?somenteAtivos=true` limita a exportação aos ativos.
 */
export async function GET(request: NextRequest) {
  const params = request.nextUrl.searchParams;
  const kind = params.get("tipo") === "modelo" ? "template" : "export";
  const onlyActive = params.get("somenteAtivos") === "true";

  const result = await downloadCollaboratorsSpreadsheet(kind, { onlyActive });

  if (!result.ok) {
    return new Response(result.message, {
      status: result.status,
      headers: { "Content-Type": "text/plain; charset=utf-8" },
    });
  }

  return new Response(result.content, {
    headers: {
      "Content-Type": XLSX_MIME,
      "Content-Disposition": `attachment; filename="${result.filename}"`,
      // Planilha reflete o banco no instante do clique: cache aqui entregaria
      // dados velhos depois de um cadastro.
      "Cache-Control": "no-store",
    },
  });
}
