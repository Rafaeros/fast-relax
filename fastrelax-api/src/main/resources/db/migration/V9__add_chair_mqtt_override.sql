-- =========================================================================
-- MQTT POR CADEIRA (OVERRIDE OPCIONAL)
--
-- Por padrão toda cadeira fala com o broker global (app.mqtt.device-*, que o
-- firmware já traz embutido em config.h) — nenhuma linha aqui é necessária
-- para o caso comum. Estas colunas existem só para o caso raro de uma cadeira
-- específica precisar de outro broker (cliente com infraestrutura própria,
-- teste isolado etc.): o SYSADMIN preenche o override no cadastro da cadeira
-- e o backend grava host/porta/usuário/senha na NVS dela via POST /mqtt do
-- firmware, sem precisar recompilar nada.
--
-- Em branco = usa o padrão global. Cada campo cai no padrão individualmente
-- (host pode ser customizado com a senha padrão, por exemplo) — mesmo
-- comportamento por-campo que mqtt_config::save() já tem no firmware.
--
-- Senha cifrada (AES-GCM, mesmo CryptoService do CPF, da senha de Wi-Fi e do
-- token de dispositivo) — nunca fica em claro no banco nem volta em resposta
-- de API nenhuma.
-- =========================================================================
BEGIN;

ALTER TABLE chairs
    ADD COLUMN mqtt_host VARCHAR(255),
    ADD COLUMN mqtt_port INTEGER,
    ADD COLUMN mqtt_username VARCHAR(100),
    ADD COLUMN mqtt_password_encrypted TEXT,
    ADD COLUMN mqtt_synced_at TIMESTAMP;

COMMIT;
