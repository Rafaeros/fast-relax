-- Tolerância para iniciar ANTES do horário agendado.
--
-- Quem chega um pouco adiantado não deveria ficar parado em frente à cadeira
-- esperando o relógio virar. Complementa start_grace_minutes, que cobre o
-- atraso; esta coluna cobre a antecipação.

ALTER TABLE session_settings
    ADD COLUMN early_start_minutes INTEGER NOT NULL DEFAULT 2
        CHECK (early_start_minutes BETWEEN 0 AND 60);
