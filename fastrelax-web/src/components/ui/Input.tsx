"use client";

import type {
  InputHTMLAttributes,
  ReactNode,
  SelectHTMLAttributes,
  TextareaHTMLAttributes,
} from "react";
import { useId } from "react";
import { cn } from "@/lib/cn";

type FieldShellProps = {
  label?: string;
  /** Texto de apoio abaixo do campo. */
  hint?: string;
  /** Mensagem de erro — substitui o hint e marca aria-invalid. */
  error?: string;
  required?: boolean;
  className?: string;
  children: ReactNode;
  htmlFor?: string;
};

/** Molécula: label + controle + hint/erro, com wiring de acessibilidade. */
export function Field({
  label,
  hint,
  error,
  required,
  className,
  children,
  htmlFor,
}: FieldShellProps) {
  return (
    <div className={cn("flex flex-col gap-1.5", className)}>
      {label && (
        <label
          htmlFor={htmlFor}
          className="text-xs font-semibold tracking-wide text-ink-secondary"
        >
          {label}
          {required && <span className="ml-1 text-error-500">*</span>}
        </label>
      )}
      {children}
      {(error || hint) && (
        <p className={cn("text-xs", error ? "text-error-400" : "text-ink-tertiary")}>
          {error ?? hint}
        </p>
      )}
    </div>
  );
}

type ControlProps = {
  label?: string;
  hint?: string;
  error?: string;
  /** Ícone decorativo no início do campo. */
  leadingIcon?: ReactNode;
  containerClassName?: string;
};

export type InputProps = ControlProps &
  InputHTMLAttributes<HTMLInputElement> & {
    /** Slot no fim do campo — aceita elemento interativo (ex.: mostrar senha). */
    trailing?: ReactNode;
  };

export function Input({
  label,
  hint,
  error,
  leadingIcon,
  trailing,
  containerClassName,
  className,
  id,
  required,
  ...rest
}: InputProps) {
  const generatedId = useId();
  const inputId = id ?? generatedId;
  const describedBy = error || hint ? `${inputId}-desc` : undefined;

  return (
    <Field
      label={label}
      hint={hint}
      error={error}
      required={required}
      htmlFor={inputId}
      className={containerClassName}
    >
      <div className="relative flex items-center">
        {leadingIcon && (
          <span className="pointer-events-none absolute left-3 text-ink-tertiary [&>svg]:h-4 [&>svg]:w-4">
            {leadingIcon}
          </span>
        )}
        <input
          id={inputId}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy}
          required={required}
          className={cn("input-base", leadingIcon && "pl-10", trailing && "pr-11", className)}
          {...rest}
        />
        {trailing && <span className="absolute right-1.5 flex items-center">{trailing}</span>}
      </div>
    </Field>
  );
}

export type TextareaProps = ControlProps & TextareaHTMLAttributes<HTMLTextAreaElement>;

export function Textarea({
  label,
  hint,
  error,
  containerClassName,
  className,
  id,
  required,
  rows = 4,
  ...rest
}: TextareaProps) {
  const generatedId = useId();
  const fieldId = id ?? generatedId;

  return (
    <Field
      label={label}
      hint={hint}
      error={error}
      required={required}
      htmlFor={fieldId}
      className={containerClassName}
    >
      <textarea
        id={fieldId}
        rows={rows}
        aria-invalid={error ? true : undefined}
        required={required}
        className={cn("input-base resize-y", className)}
        {...rest}
      />
    </Field>
  );
}

export type SelectProps = ControlProps &
  SelectHTMLAttributes<HTMLSelectElement> & {
    options?: { label: string; value: string }[];
  };

export function Select({
  label,
  hint,
  error,
  containerClassName,
  className,
  id,
  required,
  options,
  children,
  ...rest
}: SelectProps) {
  const generatedId = useId();
  const fieldId = id ?? generatedId;

  return (
    <Field
      label={label}
      hint={hint}
      error={error}
      required={required}
      htmlFor={fieldId}
      className={containerClassName}
    >
      <div className="relative">
        <select
          id={fieldId}
          aria-invalid={error ? true : undefined}
          required={required}
          className={cn("input-base appearance-none pr-10", className)}
          {...rest}
        >
          {options?.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
          {children}
        </select>
        <svg
          aria-hidden="true"
          viewBox="0 0 24 24"
          className="pointer-events-none absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-tertiary"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <path d="m6 9 6 6 6-6" />
        </svg>
      </div>
    </Field>
  );
}
