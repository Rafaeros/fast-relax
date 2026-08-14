import type { HTMLAttributes, ReactNode } from "react";
import { cn } from "@/lib/cn";

export type ContainerProps = HTMLAttributes<HTMLDivElement> & {
  size?: "narrow" | "default" | "wide";
};

const CONTAINER_SIZE = {
  narrow: "max-w-3xl",
  default: "max-w-6xl",
  wide: "max-w-7xl",
} as const;

export function Container({ size = "default", className, children, ...rest }: ContainerProps) {
  return (
    <div className={cn("mx-auto w-full px-5 sm:px-8", CONTAINER_SIZE[size], className)} {...rest}>
      {children}
    </div>
  );
}

export type SectionProps = HTMLAttributes<HTMLElement> & {
  /** Fundo alternado para separar seções vizinhas. */
  tone?: "base" | "raised";
  containerSize?: ContainerProps["size"];
  /** Renderiza sem Container interno (layouts full-bleed). */
  bare?: boolean;
};

export function Section({
  tone = "base",
  containerSize = "default",
  bare = false,
  className,
  children,
  ...rest
}: SectionProps) {
  return (
    <section
      className={cn(
        "relative py-20 sm:py-28",
        tone === "raised" ? "bg-surface-nav" : "bg-surface-base",
        className,
      )}
      {...rest}
    >
      {bare ? children : <Container size={containerSize}>{children}</Container>}
    </section>
  );
}

export type SectionHeadingProps = {
  eyebrow?: string;
  title: ReactNode;
  description?: ReactNode;
  align?: "center" | "left";
  className?: string;
  /** Conteúdo à direita do título (ex.: link "ver tudo") — só no align "left". */
  action?: ReactNode;
};

export function SectionHeading({
  eyebrow,
  title,
  description,
  align = "center",
  className,
  action,
}: SectionHeadingProps) {
  const centered = align === "center";

  return (
    <div
      className={cn(
        "flex gap-6",
        centered
          ? "flex-col items-center text-center"
          : "flex-col items-start sm:flex-row sm:items-end sm:justify-between",
        className,
      )}
    >
      <div className={cn("flex flex-col gap-3", centered && "items-center")}>
        {eyebrow && <span className="eyebrow">{eyebrow}</span>}
        <h2
          className={cn(
            "font-display text-3xl leading-tight text-ink-primary sm:text-4xl",
            centered && "max-w-2xl",
          )}
        >
          {title}
        </h2>
        {description && (
          <p
            className={cn(
              "text-sm leading-relaxed text-ink-secondary sm:text-base",
              centered ? "max-w-xl" : "max-w-2xl",
            )}
          >
            {description}
          </p>
        )}
      </div>
      {action && !centered && <div className="shrink-0">{action}</div>}
    </div>
  );
}
