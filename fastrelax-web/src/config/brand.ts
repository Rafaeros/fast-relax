/**
 * Registro central de marca e assets.
 * Trocar tema/cliente = editar ESTE arquivo + a paleta em `src/app/globals.css`.
 * Nenhum componente referencia caminho de imagem diretamente.
 */
export const brand = {
  name: "physical",
  tagline: "Bem-estar corporativo com cadeiras de massagem",
  logo: {
    /** Logo horizontal (marca + tipografia) usada na navbar e no rodapé. */
    src: "/brand/logo-physical.png",
    /** Símbolo isolado, para favicon/app/mobile. */
    markSrc: "/brand/logo-physical-mark.png",
    alt: "physical",
    width: 148,
    height: 32,
  },
} as const;

/** Todas as imagens do site. Nome do arquivo = contrato com o time de design. */
export const assets = {
  heroChair: "/images/hero-chair.png",
  heroBackground: "/images/hero-background.jpg",
  appMockup: "/images/app-colaborador.png",
  dashboardMockup: "/images/painel-rh.png",
  ctaBackground: "/images/cta-background.jpg",
  programs: {
    relax: "/images/programa-relax.jpg",
    shiatsu: "/images/programa-shiatsu.jpg",
    zeroGravity: "/images/programa-zero-gravity.jpg",
  },
} as const;
