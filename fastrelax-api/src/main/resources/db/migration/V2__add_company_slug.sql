-- Slug da empresa: identificador curto que o colaborador digita no login em
-- vez do CNPJ (ninguém decora 14 dígitos, e a mesma empresa cadastrada duas
-- vezes não pode gerar dois slugs iguais).
--
-- unaccent entra só para o backfill abaixo: "Salão" e "Salao" têm que cair no
-- mesmo slug, e sem a extensão o "ã" sobraria como caractere inválido.
CREATE EXTENSION IF NOT EXISTS unaccent;

ALTER TABLE companies ADD COLUMN slug VARCHAR(60);

-- Deriva da primeira palavra do nome (ex.: "Lanx Cables" -> "lanx"), com
-- sufixo numérico para quem colidir — mesma regra que o cadastro novo usa.
WITH candidates AS (
    SELECT
        id,
        COALESCE(
            NULLIF(
                regexp_replace(
                    regexp_replace(lower(unaccent(split_part(name, ' ', 1))), '[^a-z0-9]+', '-', 'g'),
                    '(^-+|-+$)', '', 'g'
                ),
                ''
            ),
            'empresa'
        ) AS stem
    FROM companies
),
numbered AS (
    SELECT
        id,
        stem,
        row_number() OVER (PARTITION BY stem ORDER BY id) AS rn
    FROM candidates
)
UPDATE companies AS c
SET slug = CASE WHEN numbered.rn = 1 THEN numbered.stem ELSE numbered.stem || '-' || numbered.rn END
FROM numbered
WHERE c.id = numbered.id;

ALTER TABLE companies ALTER COLUMN slug SET NOT NULL;
ALTER TABLE companies ADD CONSTRAINT uq_companies_slug UNIQUE (slug);
