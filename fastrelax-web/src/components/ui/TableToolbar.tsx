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
  const primaryAction =
    action ??
    (actionLabel && (
      <Button size="sm" leadingIcon={<Icon name={actionIcon} className="h-4 w-4" />}>
        {actionLabel}
      </Button>
    ));

  return (
    // No mobile a barra empilha: busca em uma linha, ações na seguinte. Espremer
    // tudo lado a lado deixaria o campo estreito demais para digitar.
    <div className={cn("flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-end", className)}>
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

      {/*
        Os botões dividem a largura no mobile ([&>*]:flex-1) para virarem alvos
        de toque largos, e voltam ao tamanho natural a partir de sm.
      */}
      <div className="flex items-center gap-2 [&>*]:flex-1 sm:[&>*]:flex-none">
        {filter}
        {children}
        {primaryAction}
      </div>
    </div>
  );
}
