-- =========================================================================
-- COTA DE MASSAGENS POR COLABORADOR
--
-- "Uma sessão ativa por colaborador" era uma regra do produto gravada em
-- índice. Vale para parte dos clientes; para quem contrata uma massagem por
-- semana, ou duas por dia, ela é só um bloqueio sem motivo.
--
-- Passa a ser acordo de contrato: um limite e o período em que ele conta.
-- 'ACTIVE' reproduz o comportamento anterior — N massagens marcadas ao mesmo
-- tempo, sem janela de calendário — e é o padrão, então nenhuma empresa muda
-- de regra ao aplicar esta migração.
-- =========================================================================
BEGIN;

ALTER TABLE company_session_settings
    ADD COLUMN session_quota_limit INTEGER NOT NULL DEFAULT 1
        CHECK (session_quota_limit BETWEEN 1 AND 20),
    ADD COLUMN session_quota_period VARCHAR(10) NOT NULL DEFAULT 'ACTIVE'
        CHECK (session_quota_period IN ('ACTIVE', 'DAY', 'WEEK', 'MONTH'));

-- O índice codificava limite 1 para todo mundo: com a cota por empresa, ele
-- recusaria a segunda massagem de quem contratou duas. A partir daqui quem
-- conta é a aplicação, no requireWithinQuota.
--
-- O que se perde: a checagem passa a ser antes do commit, então dois pedidos
-- simultâneos do mesmo colaborador podem passar juntos. O que continua
-- protegido no banco é o que realmente machuca — uq_session_no_overlap impede
-- duas sessões na mesma cadeira e no mesmo horário.
DROP INDEX IF EXISTS uq_collaborator_active_session;

COMMIT;
