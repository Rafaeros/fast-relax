-- =========================================================================
-- HORÁRIO PERMITIDO
--
-- A janela deixou de ser "o almoço" e passou a ser qualquer faixa em que o
-- colaborador pode agendar sessão. Só o nome muda: os dados existentes seguem
-- válidos, então renomear preserva o histórico sem migração de conteúdo.
-- =========================================================================
ALTER TABLE collaborator_work_schedules
    RENAME COLUMN lunch_start_time TO allowed_start_time;

ALTER TABLE collaborator_work_schedules
    RENAME COLUMN lunch_end_time TO allowed_end_time;
