import {
  Badge,
  Button,
  Card,
  CardBody,
  CardDescription,
  CardHeader,
  CardMedia,
  CardTitle,
  Icon,
  Media,
  Section,
  SectionHeading,
  SpecItem,
} from "@/components/ui";
import { assets } from "@/config/brand";

const PROGRAMS = [
  {
    name: "Relax Express",
    image: assets.programs.relax,
    duration: "10 min",
    badge: "Mais usado",
    description: "Pausa curta para pescoço e ombros, ideal entre reuniões.",
    specs: [
      { icon: <Icon name="clock" />, label: "Duração", value: "10 min" },
      { icon: <Icon name="leaf" />, label: "Intensidade", value: "Leve" },
      { icon: <Icon name="heart" />, label: "Foco", value: "Cervical" },
    ],
  },
  {
    name: "Shiatsu Profundo",
    image: assets.programs.shiatsu,
    duration: "20 min",
    badge: null,
    description: "Pressão progressiva na coluna para quem passa o dia sentado.",
    specs: [
      { icon: <Icon name="clock" />, label: "Duração", value: "20 min" },
      { icon: <Icon name="leaf" />, label: "Intensidade", value: "Média" },
      { icon: <Icon name="heart" />, label: "Foco", value: "Lombar" },
    ],
  },
  {
    name: "Zero Gravity",
    image: assets.programs.zeroGravity,
    duration: "30 min",
    badge: "Recuperação",
    description: "Reclínio total com massagem de corpo inteiro para desligar de vez.",
    specs: [
      { icon: <Icon name="clock" />, label: "Duração", value: "30 min" },
      { icon: <Icon name="leaf" />, label: "Intensidade", value: "Alta" },
      { icon: <Icon name="heart" />, label: "Foco", value: "Corpo inteiro" },
    ],
  },
];

export function Programs() {
  return (
    <Section containerSize="wide">
      <SectionHeading
        align="left"
        eyebrow="Programas"
        title="Uma cadeira, três formas de descansar"
        description="O colaborador escolhe o programa no app e a cadeira já inicia configurada."
        action={
          <Button variant="link" trailingIcon={<Icon name="arrowRight" className="h-4 w-4" />}>
            Ver todos os programas
          </Button>
        }
      />

      <div className="mt-12 grid gap-5 md:grid-cols-2 lg:grid-cols-3">
        {PROGRAMS.map((program) => (
          <Card key={program.name} padding="none" interactive className="flex h-full flex-col">
            <CardMedia>
              <Media
                src={program.image}
                alt={`Programa ${program.name}`}
                aspect="aspect-[16/10]"
                sizes="(max-width: 768px) 100vw, 33vw"
                overlay
              />
              {program.badge && (
                <Badge className="absolute left-4 top-4">{program.badge}</Badge>
              )}
            </CardMedia>

            <div className="flex flex-1 flex-col gap-4 p-6">
              <CardHeader>
                <CardTitle>{program.name}</CardTitle>
                <span className="font-display text-lg text-accent-soft">{program.duration}</span>
              </CardHeader>

              <CardBody className="flex-1 gap-4">
                <CardDescription>{program.description}</CardDescription>
                <div className="grid grid-cols-1 gap-2 border-t border-line pt-4">
                  {program.specs.map((spec) => (
                    <SpecItem key={spec.label} {...spec} />
                  ))}
                </div>
              </CardBody>
            </div>
          </Card>
        ))}
      </div>
    </Section>
  );
}
