-- E-mail duplicado deixou de ser proibido: a identidade do colaborador
-- continua sendo só o CPF (uq_collaborators_company_cpf). Recuperação de senha
-- por e-mail passou a resolver o mais recente entre os duplicados em vez de
-- depender de um índice único para não colidir.
DROP INDEX IF EXISTS uq_collaborators_company_email;

-- Telefone deixou de ser obrigatório: importação e cadastro manual agora
-- reconciliam como já faziam com o e-mail — em branco mantém o que já existia,
-- e cadastro novo sem telefone entra sem ele.
ALTER TABLE collaborators ALTER COLUMN phone_number DROP NOT NULL;
