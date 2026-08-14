"use client";

import Image from "next/image";
import { useState } from "react";
import { brand } from "@/config/brand";
import { cn } from "@/lib/cn";

export type LogoProps = {
  className?: string;
  /** Altura em px da marca renderizada. */
  height?: number;
  priority?: boolean;
  /** `full` = símbolo + nome. `mark` = só o símbolo (sidebar recolhida, mobile). */
  variant?: "full" | "mark";
};

/**
 * Marca do produto. Enquanto os SVGs não existirem em /public/brand,
 * cai para um wordmark tipográfico com a mesma paleta.
 */
export function Logo({ className, height = 30, priority, variant = "full" }: LogoProps) {
  const [failed, setFailed] = useState(false);
  const isMark = variant === "mark";

  const source = isMark ? brand.logo.markSrc : brand.logo.src;
  const width = isMark ? height : Math.round((brand.logo.width / brand.logo.height) * height);

  if (failed) {
    return (
      <span className={cn("flex items-center gap-2.5", className)}>
        <span
          aria-hidden="true"
          className="grid shrink-0 place-items-center rounded-full bg-gradient-to-br from-copper-300 to-copper-700 font-bold text-ink-inverse"
          style={{ height, width: height, fontSize: height * 0.45 }}
        >
          p
        </span>
        {!isMark && (
          <span className="text-lg font-semibold tracking-tight text-ink-primary">
            {brand.name}
          </span>
        )}
        {isMark && <span className="sr-only">{brand.name}</span>}
      </span>
    );
  }

  return (
    <Image
      src={source}
      alt={brand.logo.alt}
      width={width}
      height={height}
      priority={priority}
      onError={() => setFailed(true)}
      className={cn("w-auto", className)}
      style={{ height }}
    />
  );
}
