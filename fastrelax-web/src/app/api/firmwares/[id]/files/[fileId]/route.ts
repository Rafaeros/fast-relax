import type { NextRequest } from "next/server";
import { fetchFirmwareFileContent } from "@/features/firmwares/services/firmware.service";

/**
 * Ponte de download dos binários de firmware.
 *
 * <p>
 * O arquivo vem da API autenticado por Bearer, mas o token mora em cookie
 * httpOnly — inacessível ao JavaScript da página. Este handler roda no
 * servidor, resolve o token e devolve os bytes, então tanto um link comum
 * quanto o `fetch` do gravador via Web Serial conseguem chegar ao arquivo.
 *
 * <p>
 * `?nome=` só rotula o download; quem decide o conteúdo é o par de ids.
 */
export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ id: string; fileId: string }> },
) {
  const { id, fileId } = await params;

  const firmwareId = Number(id);
  const firmwareFileId = Number(fileId);

  if (!Number.isInteger(firmwareId) || !Number.isInteger(firmwareFileId)) {
    return new Response("Identificadores inválidos", { status: 400 });
  }

  const result = await fetchFirmwareFileContent(firmwareId, firmwareFileId);

  if (!result.ok) {
    return new Response(result.message, {
      status: 502,
      headers: { "Content-Type": "text/plain; charset=utf-8" },
    });
  }

  const fileName = request.nextUrl.searchParams.get("nome") ?? `firmware-${firmwareFileId}.bin`;

  return new Response(result.data, {
    headers: {
      "Content-Type": "application/octet-stream",
      "Content-Disposition": `attachment; filename="${fileName}"`,
      // O binário é imutável, mas o cache do navegador não sabe disso e o
      // arquivo pode ser substituído por outro com o mesmo id na mesma versão.
      "Cache-Control": "no-store",
    },
  });
}
