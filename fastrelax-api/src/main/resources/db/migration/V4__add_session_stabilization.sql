-- Intervalo mínimo entre o fim de uma sessão e o início da próxima na mesma
-- cadeira: o relé precisa de um tempo para desarmar e a poltrona para
-- estabilizar antes do próximo ciclo, então back-to-back sem folga nenhuma
-- não é seguro mesmo que o horário "encaixe" no papel.
ALTER TABLE company_session_settings
    ADD COLUMN stabilization_minutes INTEGER NOT NULL DEFAULT 1
    CHECK (stabilization_minutes BETWEEN 0 AND 30);
