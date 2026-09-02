"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { Icon } from "@/components/ui/Icon";
import type { IconName } from "@/components/ui/Icon";
import { cn } from "@/lib/cn";

export type ToastTone = "success" | "error" | "warning" | "info";

export type ToastOptions = {
  tone?: ToastTone;
  /** Milissegundos até sumir sozinho. `0` mantém até o usuário fechar. */
  duration?: number;
};

type Toast = {
  id: number;
  message: string;
  tone: ToastTone;
  duration: number;
};

type ToastContextValue = {
  /** Empilha um aviso. Use os atalhos abaixo para o caso comum. */
  toast: (message: string, options?: ToastOptions) => void;
  success: (message: string) => void;
  error: (message: string) => void;
};

const ToastContext = createContext<ToastContextValue | null>(null);

const TONE_STYLES: Record<ToastTone, { icon: IconName; className: string }> = {
  success: { icon: "check", className: "border-success-700 bg-success-bg text-success-400" },
  error: { icon: "alert", className: "border-error-700 bg-error-bg text-error-400" },
  warning: { icon: "alert", className: "border-warning-700 bg-warning-bg text-warning-400" },
  info: { icon: "bell", className: "border-line bg-surface-card text-ink-secondary" },
};

const DEFAULT_DURATION = 5000;

/**
 * Avisos transitórios vindos do servidor.
 *
 * <p>
 * Erros de regra de negócio ("horário já ocupado", "sessão em andamento") não
 * pertencem a um campo de formulário, então mostrá-los dentro do modal obriga
 * a manter o modal aberto só para exibir o texto. O toast desacopla as duas
 * coisas: a ação fecha o que tinha de fechar e o aviso aparece por cima.
 *
 * <p>
 * Erro de validação de campo continua no formulário — ali a mensagem precisa
 * ficar ao lado do input que o usuário vai corrigir.
 */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const nextId = useRef(0);
  const layerRef = useRef<HTMLDivElement & HTMLDialogElement>(null);
  // Assume suportado até o efeito abaixo confirmar do lado do cliente — o
  // primeiro toast só chega bem depois da hidratação, então o valor de SSR
  // não tem chance de aparecer errado na tela.
  const [popoverSupported, setPopoverSupported] = useState(true);

  useEffect(() => {
    setPopoverSupported(typeof HTMLElement !== "undefined" && "showPopover" in HTMLElement.prototype);
  }, []);

  const dismiss = useCallback((id: number) => {
    setToasts((current) => current.filter((entry) => entry.id !== id));
  }, []);

  const toast = useCallback((message: string, options: ToastOptions = {}) => {
    const id = nextId.current++;
    setToasts((current) => [
      ...current,
      {
        id,
        message,
        tone: options.tone ?? "info",
        duration: options.duration ?? DEFAULT_DURATION,
      },
    ]);
  }, []);

  /*
    O Modal usa <dialog>.showModal(), que sobe o elemento para a top layer do
    browser — lá o z-index do resto da página não alcança. Como popover, o
    container do toast entra na mesma camada, sem herdar a modalidade do
    <dialog> (fica sem backdrop, sem prender o foco, sem travar o resto da
    página).

    Mostrar só quando há aviso (e esconder ao esvaziar) importa: a ordem dentro
    da top layer é a ordem de promoção, então promover na hora do aviso deixa o
    toast acima de um modal que já estava aberto.

    Popover tem suporte mais novo que <dialog> — um WebView Android antigo ou
    Safari < 17 abrem o Modal (que já funciona há anos) mas não reconhecem
    `popover`, e o toast sem essa API fica preso no stacking context normal,
    sempre atrás do modal aberto. Nesses casos o fallback usa o próprio
    <dialog>: perde a independência (a página fica inerte enquanto o aviso
    aparece), mas garante que o toast pelo menos é visto.
  */
  useEffect(() => {
    const layer = layerRef.current;
    if (!layer) return;

    if (popoverSupported) {
      const open = layer.matches(":popover-open");
      if (toasts.length > 0 && !open) {
        layer.showPopover();
      } else if (toasts.length === 0 && open) {
        layer.hidePopover();
      }
      return;
    }

    if (toasts.length > 0 && !layer.open) {
      layer.showModal();
    } else if (toasts.length === 0 && layer.open) {
      layer.close();
    }
  }, [toasts.length, popoverSupported]);

  const value = useMemo<ToastContextValue>(
    () => ({
      toast,
      success: (message) => toast(message, { tone: "success" }),
      error: (message) => toast(message, { tone: "error" }),
    }),
    [toast],
  );

  return (
    <ToastContext.Provider value={value}>
      {children}

      {/*
        Região viva separada e sempre montada: o container visual entra e sai do
        DOM renderizado (popover), e leitor de tela costuma perder o anúncio de
        um live region que aparece junto com o texto.
      */}
      <div aria-live="polite" aria-atomic="false" className="sr-only">
        {toasts.map((entry) => (
          <p key={entry.id}>{entry.message}</p>
        ))}
      </div>

      {/*
        O container fica acima da barra de abas do mobile — sobrepor o toast à
        navegação esconderia os dois. As classes de reset (borda, fundo, padding,
        tamanho, margem) desfazem o estilo padrão que o browser aplica a
        [popover] e a <dialog>; sem elas o aviso ganha moldura branca e vai
        para o centro.
      */}
      {popoverSupported ? (
        <div ref={layerRef} popover="manual" className={LAYER_CLASS} style={LAYER_STYLE}>
          {toasts.map((entry) => (
            <ToastItem key={entry.id} toast={entry} onDismiss={dismiss} />
          ))}
        </div>
      ) : (
        <dialog
          ref={layerRef}
          aria-label="Avisos"
          // O fechamento é decidido pelo estado dos toasts, não pelo usuário —
          // sem isto, ESC fecharia o <dialog> e deixaria a página inerte (modal
          // "aberto" para o browser) enquanto o React ainda acha que há avisos.
          onCancel={(event) => event.preventDefault()}
          className={cn(LAYER_CLASS, "max-w-none backdrop:bg-transparent")}
          style={LAYER_STYLE}
        >
          {toasts.map((entry) => (
            <ToastItem key={entry.id} toast={entry} onDismiss={dismiss} />
          ))}
        </dialog>
      )}
    </ToastContext.Provider>
  );
}

const LAYER_CLASS =
  "pointer-events-none fixed inset-x-0 top-auto z-[60] m-0 h-auto w-auto overflow-visible border-0 bg-transparent p-0 px-4 text-inherit flex flex-col items-center gap-2 sm:inset-x-auto sm:right-4 sm:items-end";

const LAYER_STYLE = { bottom: "calc(4.5rem + env(safe-area-inset-bottom))" };

function ToastItem({ toast, onDismiss }: { toast: Toast; onDismiss: (id: number) => void }) {
  const { icon, className } = TONE_STYLES[toast.tone];

  useEffect(() => {
    if (toast.duration <= 0) return;
    const timer = setTimeout(() => onDismiss(toast.id), toast.duration);
    return () => clearTimeout(timer);
  }, [toast.id, toast.duration, onDismiss]);

  return (
    <div
      role="status"
      className={cn(
        "pointer-events-auto flex w-full max-w-sm items-start gap-3 rounded-card border p-3 shadow-lg",
        "animate-[toast-in_180ms_ease-out]",
        className,
      )}
    >
      <Icon name={icon} className="mt-0.5 h-4 w-4 shrink-0" />
      <p className="flex-1 text-sm leading-snug">{toast.message}</p>
      <button
        type="button"
        onClick={() => onDismiss(toast.id)}
        aria-label="Fechar aviso"
        className="shrink-0 opacity-60 transition-opacity hover:opacity-100"
      >
        <Icon name="close" className="h-4 w-4" />
      </button>
    </div>
  );
}

/** Acesso aos avisos. Precisa estar sob o {@link ToastProvider}. */
export function useToast(): ToastContextValue {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error("useToast precisa estar dentro de <ToastProvider>");
  }
  return context;
}
