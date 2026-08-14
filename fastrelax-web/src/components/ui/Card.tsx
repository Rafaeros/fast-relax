import type { HTMLAttributes, ReactNode } from "react";
import { cn } from "@/lib/cn";

export type CardVariant = "default" | "elevated" | "outline" | "glass";
export type CardPadding = "none" | "sm" | "md" | "lg";

const VARIANT: Record<CardVariant, string> = {
  default: "bg-surface-card border border-line shadow-card",
  elevated: "bg-surface-card border border-line-soft shadow-card-hover",
  outline: "bg-transparent border border-line",
  glass: "bg-surface-card/60 border border-line-soft backdrop-blur-md",
};

const PADDING: Record<CardPadding, string> = {
  none: "p-0",
  sm: "p-4",
  md: "p-6",
  lg: "p-8",
};

export type CardProps = HTMLAttributes<HTMLDivElement> & {
  variant?: CardVariant;
  padding?: CardPadding;
  /** Realce de borda/sombra no hover — use em cards clicáveis. */
  interactive?: boolean;
};

export function Card({
  variant = "default",
  padding = "md",
  interactive = false,
  className,
  children,
  ...rest
}: CardProps) {
  return (
    <div
      className={cn(
        "rounded-card overflow-hidden",
        "transition-[box-shadow,border-color,transform] duration-200 ease-out",
        VARIANT[variant],
        PADDING[padding],
        interactive &&
          "hover:border-accent-strong/60 hover:shadow-card-hover hover:-translate-y-0.5",
        className,
      )}
      {...rest}
    >
      {children}
    </div>
  );
}

/** Área de mídia do card (imagem/ilustração) com proporção fixa. */
export function CardMedia({
  className,
  children,
  ...rest
}: HTMLAttributes<HTMLDivElement>) {
  return (
    <div className={cn("relative w-full overflow-hidden bg-bg-900", className)} {...rest}>
      {children}
    </div>
  );
}

export function CardHeader({ className, children, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div className={cn("flex items-start justify-between gap-4", className)} {...rest}>
      {children}
    </div>
  );
}

export function CardTitle({ className, children, ...rest }: HTMLAttributes<HTMLHeadingElement>) {
  return (
    <h3 className={cn("text-base font-semibold text-ink-primary", className)} {...rest}>
      {children}
    </h3>
  );
}

export function CardDescription({
  className,
  children,
  ...rest
}: HTMLAttributes<HTMLParagraphElement>) {
  return (
    <p className={cn("text-sm leading-relaxed text-ink-secondary", className)} {...rest}>
      {children}
    </p>
  );
}

export function CardBody({ className, children, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div className={cn("flex flex-col gap-2", className)} {...rest}>
      {children}
    </div>
  );
}

export function CardFooter({ className, children, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn("flex items-center justify-between gap-3 border-t border-line pt-4", className)}
      {...rest}
    >
      {children}
    </div>
  );
}

/** Molécula: card de destaque com ícone + título + texto (grid de benefícios). */
export function FeatureCard({
  icon,
  title,
  description,
  className,
}: {
  icon: ReactNode;
  title: string;
  description: string;
  className?: string;
}) {
  return (
    <Card variant="default" padding="lg" interactive className={cn("h-full", className)}>
      <div className="flex flex-col gap-4">
        <span className="text-accent-soft [&>svg]:h-8 [&>svg]:w-8">{icon}</span>
        <CardTitle>{title}</CardTitle>
        <CardDescription>{description}</CardDescription>
      </div>
    </Card>
  );
}
