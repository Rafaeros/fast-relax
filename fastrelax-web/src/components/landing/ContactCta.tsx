"use client";

import type { FormEvent } from "react";
import { Button, Card, Icon, Input, Section, SectionHeading, Select } from "@/components/ui";

const COMPANY_SIZES = [
  { label: "Até 100 colaboradores", value: "ate-100" },
  { label: "101 a 500 colaboradores", value: "101-500" },
  { label: "501 a 2.000 colaboradores", value: "501-2000" },
  { label: "Mais de 2.000 colaboradores", value: "2000-mais" },
];

export function ContactCta() {
  // Sem back-end conectado ainda — evita reload até existir a rota de contato.
  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
  };

  return (
    <Section tone="raised" containerSize="wide">
      <div className="grid gap-12 lg:grid-cols-[1fr_1.1fr] lg:gap-16">
        <SectionHeading
          align="left"
          className="lg:flex-col lg:items-start"
          eyebrow="Fale com a gente"
          title="Leve a pausa para o seu time"
          description="Conte quantos colaboradores você tem e montamos a proposta de implantação, com quantidade de cadeiras e plano de agenda."
        />

        <Card padding="lg">
          <form className="flex flex-col gap-5" onSubmit={handleSubmit}>
            <div className="grid gap-5 sm:grid-cols-2">
              <Input label="Nome" name="nome" placeholder="Seu nome" required />
              <Input label="Empresa" name="empresa" placeholder="Nome da empresa" required />
            </div>

            <div className="grid gap-5 sm:grid-cols-2">
              <Input
                label="E-mail corporativo"
                name="email"
                type="email"
                placeholder="voce@empresa.com"
                required
              />
              <Input
                label="Telefone"
                name="telefone"
                type="tel"
                placeholder="(11) 90000-0000"
                hint="WhatsApp de preferência"
              />
            </div>

            <Select label="Tamanho do time" name="porte" options={COMPANY_SIZES} />

            <Button
              type="submit"
              size="lg"
              fullWidth
              trailingIcon={<Icon name="arrowRight" className="h-4 w-4" />}
            >
              Solicitar proposta
            </Button>

            <p className="text-xs text-ink-tertiary">
              Resposta em até um dia útil. Seus dados não são compartilhados com terceiros.
            </p>
          </form>
        </Card>
      </div>
    </Section>
  );
}
