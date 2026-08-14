import type { HTMLAttributes, ReactNode } from "react";
import { cn } from "@/lib/cn";
import { Icon } from "@/components/ui/Icon";
import type { IconName } from "@/components/ui/Icon";

export type AlertTone = "error" | "warning" | "success" | "info";

const TONE: Record<AlertTone, { classes: string; icon: IconName }> = {
  error: { classes: "badge-error", icon: "alert" },
  warning: { classes: "badge-warning", icon: "alert" },
  success: { classes: "badge-success", icon: "check" },
  info: { classes: "badge-info", icon: "alert" },
};

export type AlertProps = HTMLAttributes<HTMLDivElement> & {
  tone?: AlertTone;
  title?: string;
  /** Lista de detalhes (ex.: `errors[]` devolvido pela API). */
  details?: string[];
  children?: ReactNode;
};

export function Alert({
  tone = "error",
  title,
  details,
  className,
  children,
  ...rest
}: AlertProps) {
  const { classes, icon } = TONE[tone];

  return (
    <div
      role={tone === "error" ? "alert" : "status"}
      className={cn("flex gap-3 rounded-control border p-3.5 text-sm", classes, className)}
      {...rest}
    >
      <Icon name={icon} className="mt-0.5 h-4 w-4 shrink-0" />
      <div className="flex flex-col gap-1">
        {title && <span className="font-semibold">{title}</span>}
        {children && <span className="leading-relaxed">{children}</span>}
        {details && details.length > 0 && (
          <ul className="list-inside list-disc text-xs opacity-90">
            {details.map((detail) => (
              <li key={detail}>{detail}</li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
