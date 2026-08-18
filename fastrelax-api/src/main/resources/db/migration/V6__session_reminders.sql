-- =========================================================
-- Marcas de lembrete por sessão
-- =========================================================
-- `collaborator_sessions.reminder_sent_at` guardava um carimbo só, o que basta
-- para um lembrete e falha para três: enviar o de 1 hora marcaria a sessão como
-- lembrada e engoliria o de 5 minutos.
--
-- Uma linha por (sessão, faixa) resolve: as faixas passam a ser independentes, e
-- acrescentar uma nova amanhã é mudar a configuração, não migrar tabela.
CREATE TABLE session_reminders (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    session_id BIGINT NOT NULL,

    -- 'DAY_BEFORE' para o resumo diário; 'T-60', 'T-5' para os rolantes. O
    -- formato carrega o offset, então a faixa nova não exige nada do banco.
    kind VARCHAR(20) NOT NULL,

    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- A sessão some, as marcas somem junto: sem a sessão elas não significam nada.
    FOREIGN KEY (session_id) REFERENCES collaborator_sessions(id) ON DELETE CASCADE
);

-- Barreira de verdade contra lembrete repetido: se duas execuções se cruzarem,
-- a segunda esbarra aqui em vez de mandar push duplicado.
CREATE UNIQUE INDEX uq_session_reminders_session_kind
ON session_reminders (session_id, kind);

ALTER TABLE collaborator_sessions DROP COLUMN reminder_sent_at;
