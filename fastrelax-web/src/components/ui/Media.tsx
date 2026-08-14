"use client";

import Image from "next/image";
import { useState } from "react";
import { cn } from "@/lib/cn";
import { Icon } from "@/components/ui/Icon";

export type MediaProps = {
  src: string;
  alt: string;
  /** Proporção do container (classe Tailwind, ex.: "aspect-[4/3]"). */
  aspect?: string;
  className?: string;
  imageClassName?: string;
  priority?: boolean;
  sizes?: string;
  /** Ajuste da imagem no container. "contain" para PNG recortado da cadeira. */
  fit?: "cover" | "contain";
  /** Véu escuro por cima (legibilidade de texto sobreposto). */
  overlay?: boolean;
};

/**
 * Imagem com placeholder automático: se o arquivo ainda não existe em /public,
 * mostra a moldura com o caminho esperado em vez de quebrar o layout.
 */
export function Media({
  src,
  alt,
  aspect = "aspect-[4/3]",
  className,
  imageClassName,
  priority,
  sizes = "(max-width: 768px) 100vw, 50vw",
  fit = "cover",
  overlay = false,
}: MediaProps) {
  const [failed, setFailed] = useState(false);

  return (
    <div className={cn("relative w-full overflow-hidden bg-bg-900", aspect, className)}>
      {failed ? (
        <div className="absolute inset-0 flex flex-col items-center justify-center gap-2 border border-dashed border-line bg-bg-900 p-4 text-center">
          <Icon name="chair" className="h-7 w-7 text-ink-tertiary" />
          <span className="text-[0.6875rem] font-semibold uppercase tracking-[0.18em] text-ink-tertiary">
            imagem pendente
          </span>
          <code className="text-[0.6875rem] text-ink-muted">{src}</code>
        </div>
      ) : (
        <Image
          src={src}
          alt={alt}
          fill
          priority={priority}
          sizes={sizes}
          onError={() => setFailed(true)}
          className={cn(fit === "cover" ? "object-cover" : "object-contain", imageClassName)}
        />
      )}
      {overlay && !failed && (
        <div className="absolute inset-0 bg-gradient-to-t from-bg-950 via-bg-950/45 to-transparent" />
      )}
    </div>
  );
}
