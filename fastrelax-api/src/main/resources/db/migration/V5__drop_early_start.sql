-- A antecedência de início saiu.
--
-- Iniciar antes do horário agendado obrigava a escolher entre duas massagens
-- erradas: parar no fim agendado (sessão mais curta que a duração contratada)
-- ou rodar a duração cheia a partir do início real (invadindo a folga de
-- estabilização e o horário da próxima pessoa na mesma cadeira). Sem
-- antecipação, o início real nunca é anterior ao agendado e a grade volta a
-- ser previsível: cada linha ocupa exatamente a faixa que reservou.
--
-- Atraso continua tolerado por start_grace_minutes, e nesse caso a massagem
-- termina no horário final agendado — quem chega tarde perde os minutos, a
-- próxima sessão não.
ALTER TABLE company_session_settings
    DROP COLUMN early_start_minutes;
