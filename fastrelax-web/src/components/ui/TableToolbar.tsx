"use client";

import type { ReactNode } from "react";
import { cn } from "@/lib/cn";
import { Button } from "@/components/ui/Button";
import { Icon } from "@/components/ui/Icon";
import type { IconName } from "@/components/ui/Icon";
import { IconButton } from "@/components/ui/IconButton";
import { Input } from "@/components/ui/Input";

export type TableToolbarProps = {
  /** Placeholder da busca — descreve o que dá para procurar naquela lista. */
  searchPlaceholder: string;
  /** Valor controlado da busca. Sem `onSearchChange` o campo não é renderizado. */
  searchValue?: string;
  onSearchChange?: (value: string) => void;
  /**
   * Botão de filtro — normalmente o componente de modal da própria feature,
   * que conhece os campos filtráveis daquele recurso.
   */
  filter?: ReactNode;
  /** Rótulo do botão principal padrão (ex.: "Novo usuário"). */
  actionLabel?: string;
  actionIcon?: IconName;
  /**
   * Substitui o botão principal — para quando a ação abre um modal e precisa
   * do próprio estado (ex.: `CreateCollaboratorModal`).
   */
  action?: ReactNode;
  /** Ações extras entre o filtro e o botão principal. */
  children?: ReactNode;
  className?: string;
};

/**
 * Barra de ações padrão das listagens: busca, filtro e ação primária.
 * Vai no slot `toolbar` do `DataTable` — mesma ordem e mesmo espaçamento em
 * todas as telas, sem cada página inventar seu próprio arranjo.
 */
export function TableToolbar({
  searchPlaceholder,
  searchValue,
  onSearchChange,
  filter,
  actionLabel,
  actionIcon = "sparkle",
  action,
  children,
  className,
}: TableToolbarProps) {
  return (
    <div className={cn("flex flex-wrap items-center justify-end gap-2", className)}>
      {onSearchChange && (
        <Input
          name="q"
          type="search"
          aria-label={searchPlaceholder}
          placeholder={searchPlaceholder}
          value={searchValue ?? ""}
          onChange={(event) => onSearchChange(event.target.value)}
          leadingIcon={<Icon name="search" />}
          containerClassName="w-full sm:w-72"
          className="py-2"
          trailing={
            searchValue ? (
              <IconButton
                label="Limpar busca"
                onClick={() => onSearchChange("")}
                icon={<Icon name="close" className="h-4 w-4" />}
              />
            ) : undefined
          }
        />
      )}

      {filter}
      {children}

      {action ??
        (actionLabel && (
          <Button size="sm" leadingIcon={<Icon name={actionIcon} className="h-4 w-4" />}>
            {actionLabel}
          </Button>
        ))}
    </div>
  );
}
