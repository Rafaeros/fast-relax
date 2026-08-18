import type { Metadata, Viewport } from "next";
import { Geist, Geist_Mono, Playfair_Display } from "next/font/google";
import "./globals.css";
import { ToastProvider } from "@/components/ui";
import { brand } from "@/config/brand";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

// Serifada de display — usada nos títulos (token --font-display no globals.css).
const displaySerif = Playfair_Display({
  variable: "--font-display-serif",
  subsets: ["latin"],
  weight: ["400", "500", "600"],
});

export const metadata: Metadata = {
  title: `${brand.name} — ${brand.tagline}`,
  description:
    "Cadeiras de massagem profissionais para empresas, com app de agendamento para colaboradores e painel de RH com dashboards de bem-estar.",
};

/**
 * `viewportFit: "cover"` faz o conteúdo ocupar a tela inteira em aparelhos com
 * notch ou barra de gestos, e é o que habilita as variáveis `env(safe-area-*)`
 * usadas no header e na barra de abas. Sem isso, o app empacotado pelo Capacitor
 * ganha faixas pretas nas bordas.
 *
 * O zoom fica liberado (`maximumScale` ausente) de propósito: travar pinça
 * quebra acessibilidade para quem precisa ampliar.
 */
export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  viewportFit: "cover",
  themeColor: "#0a0806",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html
      lang="pt-BR"
      className={`${geistSans.variable} ${geistMono.variable} ${displaySerif.variable} h-full antialiased`}
    >
      <body className="min-h-full flex flex-col font-sans">
        {/* No topo da árvore: qualquer tela pode avisar sem montar o próprio
            container de mensagens. */}
        <ToastProvider>{children}</ToastProvider>
      </body>
    </html>
  );
}
