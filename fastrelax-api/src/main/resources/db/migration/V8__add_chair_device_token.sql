-- =========================================================================
-- TOKEN POR CADEIRA
--
-- Substitui o segredo único compartilhado entre todas as cadeiras
-- (CHAIR_DEVICE_TOKEN, gravado igual em todo firmware) por um token gerado
-- pelo próprio ESP32 no primeiro boot e pareado com o backend no primeiro
-- heartbeat — confiança no primeiro contato. Dali em diante, um MAC com token
-- divergente é recusado: não basta saber o MAC para assumir a identidade da
-- cadeira.
--
-- Nula até o primeiro heartbeat pareado. Cadeiras já cadastradas antes desta
-- migração pareiam sozinhas assim que o firmware novo subir — não precisa de
-- backfill.
--
-- Cifrado, não hash: o backend precisa devolver o valor em claro a cada
-- comando (HTTP e MQTT), então é AES-GCM reversível — o mesmo CryptoService
-- do CPF e da senha de Wi-Fi — e não um digest.
-- =========================================================================
BEGIN;

ALTER TABLE chairs
    ADD COLUMN device_token_encrypted TEXT;

COMMIT;
