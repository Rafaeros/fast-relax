-- Cadeiras controladas por ESP32.
--
-- Identidade é o MAC do Wi-Fi, não o IP: o IP muda a cada renovação de DHCP e
-- serve apenas para alcançar o dispositivo. Trocar um ESP32 queimado por outro
-- é regravar o mesmo MAC — nenhuma referência de sessão precisa mudar.

CREATE TABLE chairs (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    mac_address VARCHAR(17) NOT NULL UNIQUE,
    -- Endereço de comunicação, atualizado a cada heartbeat.
    ip_address VARCHAR(45),
    port INTEGER NOT NULL DEFAULT 80 CHECK (port BETWEEN 1 AND 65535),
    last_seen_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_chairs_last_seen_at ON chairs (last_seen_at) WHERE deleted_at IS NULL;

-- Qual cadeira atendeu cada sessão. Nulo em sessões antigas e enquanto a sessão
-- não foi iniciada.
ALTER TABLE collaborator_sessions
    ADD COLUMN chair_id BIGINT REFERENCES chairs(id);
