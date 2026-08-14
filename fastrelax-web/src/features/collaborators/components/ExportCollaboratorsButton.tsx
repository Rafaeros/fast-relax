"use client";

import { Icon, buttonStyles } from "@/components/ui";

/**
 * Baixa a planilha de colaboradores no mesmo layout da importação — dá para
 * editar em massa e reenviar.
 *
 * É um link comum, não um botão com JavaScript: o Route Handler devolve os
 * bytes com `Content-Disposition`, e o browser cuida do download.
 */
export function ExportCollaboratorsButton() {
  return (
    <a
      href="/api/colaboradores/planilha"
      className={buttonStyles({ variant: "secondary", size: "sm" })}
    >
      <Icon name="download" className="h-4 w-4" />
      Exportar
    </a>
  );
}
