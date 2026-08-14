"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { Button, buttonStyles, Container, Icon, Logo } from "@/components/ui";
import { cn } from "@/lib/cn";

const NAV_LINKS = [
  { label: "Início", href: "#inicio" },
  { label: "A cadeira", href: "#cadeira" },
  { label: "Plataforma", href: "#plataforma" },
  { label: "Como funciona", href: "#como-funciona" },
  { label: "Contato", href: "#contato" },
];

export function Navbar() {
  const [scrolled, setScrolled] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 16);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  return (
    <header
      className={cn(
        "fixed inset-x-0 top-0 z-50 transition-colors duration-300",
        scrolled || menuOpen
          ? "border-b border-line/70 bg-surface-base/85 backdrop-blur-lg"
          : "border-b border-transparent bg-transparent",
      )}
    >
      <Container size="wide">
        <nav className="flex h-18 items-center justify-between gap-6 py-4">
          <a href="#inicio" className="flex items-center" aria-label={`${"physical"} — início`}>
            <Logo priority />
          </a>

          <ul className="hidden items-center gap-8 lg:flex">
            {NAV_LINKS.map((link) => (
              <li key={link.href}>
                <a
                  href={link.href}
                  className="text-[0.8125rem] font-medium uppercase tracking-[0.14em] text-ink-secondary transition-colors hover:text-ink-primary"
                >
                  {link.label}
                </a>
              </li>
            ))}
          </ul>

          <div className="flex items-center gap-2">
            <Link
              href="/entrar"
              className={buttonStyles({
                variant: "secondary",
                size: "sm",
                className: "hidden sm:inline-flex",
              })}
            >
              <Icon name="lock" className="h-4 w-4" />
              Área do cliente
            </Link>

            <Button
              variant="ghost"
              size="sm"
              className="lg:hidden"
              aria-expanded={menuOpen}
              aria-controls="menu-mobile"
              aria-label={menuOpen ? "Fechar menu" : "Abrir menu"}
              onClick={() => setMenuOpen((open) => !open)}
            >
              <Icon name={menuOpen ? "close" : "menu"} />
            </Button>
          </div>
        </nav>
      </Container>

      {menuOpen && (
        <div id="menu-mobile" className="border-t border-line bg-surface-nav lg:hidden">
          <Container size="wide">
            <ul className="flex flex-col py-4">
              {NAV_LINKS.map((link) => (
                <li key={link.href}>
                  <a
                    href={link.href}
                    onClick={() => setMenuOpen(false)}
                    className="block py-3 text-sm uppercase tracking-[0.14em] text-ink-secondary transition-colors hover:text-ink-primary"
                  >
                    {link.label}
                  </a>
                </li>
              ))}
              <li className="pt-3 sm:hidden">
                <Link
                  href="/entrar"
                  onClick={() => setMenuOpen(false)}
                  className={buttonStyles({ variant: "secondary", size: "sm", fullWidth: true })}
                >
                  <Icon name="lock" className="h-4 w-4" />
                  Área do cliente
                </Link>
              </li>
            </ul>
          </Container>
        </div>
      )}
    </header>
  );
}
