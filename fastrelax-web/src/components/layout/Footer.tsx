import { Container, Logo } from "@/components/ui";
import { brand } from "@/config/brand";

const COLUMNS = [
  {
    title: "Produto",
    links: ["A cadeira", "Programas de sessão", "App do colaborador", "Painel de RH"],
  },
  {
    title: "Empresa",
    links: ["Sobre a physical", "Casos de uso", "Manutenção e suporte", "Contato"],
  },
  {
    title: "Recursos",
    links: ["Guia de implantação", "Boas práticas de bem-estar", "Perguntas frequentes"],
  },
];

export function Footer() {
  return (
    <footer id="contato" className="border-t border-line bg-surface-nav">
      <Container size="wide" className="py-16">
        <div className="grid gap-12 lg:grid-cols-[1.4fr_repeat(3,1fr)]">
          <div className="flex flex-col gap-4">
            <Logo height={28} />
            <p className="max-w-xs text-sm leading-relaxed text-ink-secondary">
              {brand.tagline}. Pausas curtas, agendadas pelo próprio colaborador e acompanhadas pelo
              RH.
            </p>
          </div>

          {COLUMNS.map((column) => (
            <div key={column.title} className="flex flex-col gap-4">
              <h3 className="text-[0.6875rem] font-semibold uppercase tracking-[0.2em] text-ink-muted">
                {column.title}
              </h3>
              <ul className="flex flex-col gap-2.5">
                {column.links.map((link) => (
                  <li key={link}>
                    {/* Sem rota definida ainda — âncoras entram junto com as páginas internas. */}
                    <span className="text-sm text-ink-secondary transition-colors hover:text-ink-primary">
                      {link}
                    </span>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        <div className="hairline my-10" />

        <div className="flex flex-col gap-3 text-xs text-ink-tertiary sm:flex-row sm:items-center sm:justify-between">
          <span>
            © {new Date().getFullYear()} {brand.name}. Todos os direitos reservados.
          </span>
          <span className="flex gap-6">
            <span>Privacidade</span>
            <span>Termos de uso</span>
          </span>
        </div>
      </Container>
    </footer>
  );
}
