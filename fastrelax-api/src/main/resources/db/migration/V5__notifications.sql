-- =========================================================
-- NOTIFICAÇÕES: entrega (device_tokens) e histórico (notifications)
-- =========================================================

-- ---------------------------------------------------------
-- device_tokens: Web Push não usa token
-- ---------------------------------------------------------
-- FCM (Android/iOS) entrega por um token opaco. Web Push entrega por uma
-- subscription — endpoint do serviço do navegador mais duas chaves de
-- criptografia. São formatos diferentes demais para caber na mesma coluna, então
-- cada plataforma preenche o campo que lhe cabe e a checagem abaixo garante que
-- um dos dois exista.
ALTER TABLE device_tokens ALTER COLUMN token DROP NOT NULL;
ALTER TABLE device_tokens ADD COLUMN push_subscription JSONB;

-- O UNIQUE da criação virou índice parcial: com token nulo nas linhas WEB, o
-- unique comum ainda funcionaria (NULLs não colidem), mas o índice parcial não
-- carrega essas linhas à toa.
ALTER TABLE device_tokens DROP CONSTRAINT device_tokens_token_key;

CREATE UNIQUE INDEX uq_device_tokens_token
ON device_tokens (token)
WHERE token IS NOT NULL;

-- O endpoint identifica a inscrição do navegador do mesmo jeito que o token
-- identifica o aparelho: reinscrever precisa atualizar a linha existente, não
-- criar outra. Índice por expressão evita uma coluna redundante.
CREATE UNIQUE INDEX uq_device_tokens_endpoint
ON device_tokens ((push_subscription ->> 'endpoint'))
WHERE push_subscription IS NOT NULL;

ALTER TABLE device_tokens ADD CONSTRAINT ck_device_tokens_destination CHECK (
    (platform = 'WEB' AND push_subscription IS NOT NULL)
    OR (platform IN ('ANDROID', 'IOS') AND token IS NOT NULL)
);

-- ---------------------------------------------------------
-- notifications: o que foi avisado, independente da entrega
-- ---------------------------------------------------------
-- Push é entrega best-effort: aparelho desligado, permissão revogada ou
-- navegador fechado fazem a mensagem se perder sem aviso. Persistir aqui dá ao
-- app uma central de notificações que não depende de o push ter chegado.
CREATE TABLE notifications (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    collaborator_id BIGINT NOT NULL,

    type VARCHAR(50) NOT NULL CHECK (type IN (
        'SESSION_SCHEDULED',
        'SESSION_REMINDER',
        'SESSION_STARTED',
        'SESSION_FINISHED',
        'SESSION_EXPIRED',
        'SESSION_CANCELLED'
    )),
    title VARCHAR(150) NOT NULL,
    body TEXT NOT NULL,

    -- Carga livre para o clique abrir a tela certa (ex.: {"sessionId": 42}).
    data JSONB,

    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (collaborator_id) REFERENCES collaborators(id)
);

-- A listagem é sempre "as minhas, mais recentes primeiro".
CREATE INDEX idx_notifications_collaborator
ON notifications (collaborator_id, created_at DESC);

-- O contador do sininho só olha as não lidas.
CREATE INDEX idx_notifications_unread
ON notifications (collaborator_id)
WHERE read_at IS NULL;

-- ---------------------------------------------------------
-- Lembrete antes da sessão
-- ---------------------------------------------------------
-- O job roda de minuto em minuto e a mesma sessão continua elegível até
-- começar; a marca é o que impede o colaborador de receber o mesmo lembrete
-- várias vezes.
ALTER TABLE collaborator_sessions ADD COLUMN reminder_sent_at TIMESTAMP;
