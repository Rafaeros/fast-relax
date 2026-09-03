/**
 * Densidade vertical compartilhada pelas duas telas de login (painel e colaborador).
 *
 * <p>
 * Regra de ouro: no desktop (>= `lg`) a tela inteira precisa caber em 100dvh —
 * sem scroll da página e sem scroll interno da coluna do formulário. Como altura
 * é o recurso escasso lá, os espaçamentos deixam de ser fixos e passam a
 * acompanhar a viewport via `clamp(mínimo, k*dvh, máximo)`: num laptop de
 * ~640-700px úteis eles encolhem até o mínimo ainda legível, e em telas altas
 * voltam ao respiro original. Só o que é espaço em branco encolhe — altura de
 * campo, alvo de toque do botão e tamanho da letra miúda ficam intactos.
 *
 * <p>
 * Abaixo de `lg` nada disso vale: o mobile continua com `min-h-dvh` e scroll
 * normal da página, que é o comportamento certo num formulário que o dedo rola.
 *
 * <p>
 * As duas telas compartilham a mesma composição, então as classes moram aqui
 * para não divergirem — ajustar a densidade em um lugar ajusta nos dois.
 */

/** Grade externa: uma tela travada em duas colunas no desktop, fluxo normal no mobile. */
export const LOGIN_SHELL = "grid min-h-dvh lg:h-dvh lg:grid-cols-2 lg:overflow-hidden";

/**
 * Coluna do formulário.
 *
 * <p>
 * `lg:overflow-y-auto` continua aqui como rede de segurança — não como layout
 * esperado. Com a densidade abaixo o conteúdo cabe em alturas reais de laptop,
 * então a barra não aparece; ela só existe para o caso patológico (zoom em 200%,
 * janela redimensionada para uma faixa), onde esconder conteúdo seria pior.
 */
export const LOGIN_MAIN =
  "flex flex-col justify-center px-5 pt-12 pb-12 sm:px-10 lg:h-full lg:min-h-0 lg:overflow-y-auto lg:pt-[clamp(0.75rem,2.5dvh,2.5rem)] lg:pb-[clamp(0.75rem,2.5dvh,2.5rem)]";

/**
 * Variante do colaborador: o app roda dentro do Capacitor, então o topo respeita
 * a safe area. No desktop o `lg:pt-*` assume e o recorte deixa de existir.
 */
export const LOGIN_MAIN_SAFE_AREA =
  "flex flex-col justify-center px-5 pt-[calc(env(safe-area-inset-top)+3rem)] pb-12 sm:px-10 lg:h-full lg:min-h-0 lg:overflow-y-auto lg:pt-[clamp(0.75rem,2.5dvh,2.5rem)] lg:pb-[clamp(0.75rem,2.5dvh,2.5rem)]";

/** Pilha da coluna: cabeçalho, título, card e rodapé. */
export const LOGIN_STACK =
  "mx-auto flex w-full max-w-md flex-col gap-8 lg:gap-[clamp(0.875rem,2.4dvh,2rem)]";

/** Bloco de título (eyebrow + h1 + descrição). */
export const LOGIN_HEADING = "flex flex-col gap-2 lg:gap-[clamp(0.25rem,0.8dvh,0.5rem)]";

/**
 * O eyebrow some a partir de `lg` porque é aí que o painel da esquerda aparece
 * anunciando a mesma área — repetir custa uma linha inteira do orçamento
 * vertical sem acrescentar informação. No mobile, onde o painel não existe, ele
 * continua sendo a única pista de contexto e permanece visível.
 */
export const LOGIN_EYEBROW = "eyebrow lg:hidden";

export const LOGIN_TITLE =
  "font-display text-3xl text-ink-primary lg:text-[length:clamp(1.375rem,3.2dvh,1.875rem)] lg:leading-tight";

export const LOGIN_DESCRIPTION =
  "text-sm leading-relaxed text-ink-secondary lg:text-[0.8125rem] lg:leading-snug";

/**
 * Padding do card do formulário — só o override de desktop. Use junto de
 * `<Card padding="lg">`: o `cn` do projeto apenas concatena classes (não é
 * tailwind-merge), então o `p-8` do componente precisa continuar sendo a base e
 * a variante `lg:` é quem vence, sem disputa de ordem no CSS.
 */
export const LOGIN_CARD = "lg:p-[clamp(1rem,3dvh,2rem)]";

/** Espaçamento entre os campos do formulário. */
export const LOGIN_FORM = "flex flex-col gap-5 lg:gap-[clamp(0.75rem,2.2dvh,1.25rem)]";
