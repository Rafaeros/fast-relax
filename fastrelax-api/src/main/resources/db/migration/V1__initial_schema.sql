-- Schema inicial do FastRelax.
--
-- Consolida a evolução que antes vivia em V1..V10. Como o projeto ainda não foi
-- para produção, o histórico incremental não tinha valor — o schema final é mais
-- fácil de ler do que a sequência de ALTERs que levou até ele.

-- Necessária para o EXCLUDE de sessões: combina igualdade (session_date) com o
-- operador de sobreposição de ranges no mesmo índice GiST.
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- =========================================================================
-- USUÁRIOS DO PAINEL (ADMIN/RH)
-- =========================================================================
CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL CHECK (role IN (
        'ADMIN',
        'RH'
    )),
    -- Usuário criado pelo ADMIN nasce com senha temporária e precisa definir a
    -- própria no primeiro acesso; até lá o acesso fica restrito a essa troca.
    must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- =========================================================================
-- DEPARTAMENTOS
-- =========================================================================
CREATE TABLE departments (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- =========================================================================
-- COLABORADORES
--
-- O CPF é guardado de duas formas com propósitos distintos:
--   cpf_encrypted -> AES-GCM reversível, para o RH consultar. IV aleatório, então
--                    o mesmo CPF gera ciphertext diferente a cada gravação — não
--                    serve para busca nem para unicidade.
--   cpf_hash      -> HMAC determinístico (blind index). Carrega a constraint de
--                    unicidade e é por onde o login procura.
-- =========================================================================
CREATE TABLE collaborators (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    department_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    cpf_encrypted TEXT NOT NULL,
    cpf_hash TEXT NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT uq_collaborators_cpf_hash UNIQUE (cpf_hash),
    FOREIGN KEY (department_id) REFERENCES departments(id)
);

-- =========================================================================
-- HORÁRIO DE ALMOÇO
--
-- Domingo não entra: é o único dia sem expediente.
-- =========================================================================
CREATE TABLE collaborator_work_schedules (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    collaborator_id BIGINT NOT NULL,
    day_of_week VARCHAR(10) NOT NULL CHECK (day_of_week IN (
        'MONDAY',
        'TUESDAY',
        'WEDNESDAY',
        'THURSDAY',
        'FRIDAY',
        'SATURDAY'
    )),
    lunch_start_time TIME NOT NULL,
    lunch_end_time TIME NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (collaborator_id) REFERENCES collaborators(id)
);

-- Parcial para que uma linha removida não impeça recadastrar o mesmo dia.
CREATE UNIQUE INDEX uq_work_schedule_collaborator_day
ON collaborator_work_schedules (collaborator_id, day_of_week)
WHERE deleted_at IS NULL;

-- =========================================================================
-- SESSÕES DE DESCANSO
--
-- SCHEDULED -> STARTED -> DONE
--     |           |
--     |           +-- (não finalizou)     -> EXPIRED
--     +-- (não iniciou até a tolerância)  -> EXPIRED
--     +-- cancelamento manual             -> CANCELLED
-- =========================================================================
CREATE TABLE collaborator_sessions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    collaborator_id BIGINT NOT NULL,
    session_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN (
        'SCHEDULED',
        'STARTED',
        'DONE',
        'EXPIRED',
        'CANCELLED'
    )),
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (collaborator_id) REFERENCES collaborators(id)
);

-- Um colaborador só pode ter uma sessão em aberto por vez.
CREATE UNIQUE INDEX uq_collaborator_active_session
ON collaborator_sessions (collaborator_id)
WHERE status IN ('SCHEDULED', 'STARTED');

-- Só existe um recurso de atendimento, então duas sessões ativas não podem se
-- sobrepor no tempo. A checagem na aplicação roda antes do commit, então dois
-- agendamentos simultâneos passariam os dois — o banco é o único lugar capaz de
-- serializar isso.
ALTER TABLE collaborator_sessions
    ADD CONSTRAINT uq_session_no_overlap
    EXCLUDE USING gist (
        session_date WITH =,
        tsrange(
            (session_date + start_time)::timestamp,
            (session_date + end_time)::timestamp
        ) WITH &&
    )
    WHERE (status IN ('SCHEDULED', 'STARTED'));

-- =========================================================================
-- CONFIGURAÇÃO GLOBAL DAS SESSÕES
--
-- Linha única, garantida pelo índice em expressão constante.
-- =========================================================================
CREATE TABLE session_settings (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    default_duration_minutes INTEGER NOT NULL DEFAULT 5
        CHECK (default_duration_minutes BETWEEN 1 AND 120),
    start_grace_minutes INTEGER NOT NULL DEFAULT 2
        CHECK (start_grace_minutes BETWEEN 0 AND 60),
    max_advance_days INTEGER NOT NULL DEFAULT 30
        CHECK (max_advance_days BETWEEN 1 AND 365),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_session_settings_singleton ON session_settings ((TRUE));

INSERT INTO session_settings (default_duration_minutes, start_grace_minutes, max_advance_days)
VALUES (5, 2, 30);

-- =========================================================================
-- REFRESH TOKENS
--
-- Persistidos em vez de JWT autocontido: só assim é possível revogar o acesso
-- antes do vencimento. Guardamos o hash SHA-256, nunca o token em claro — um
-- vazamento do banco não pode ser usado para autenticar.
-- =========================================================================
CREATE TABLE refresh_tokens (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    token_hash TEXT NOT NULL UNIQUE,
    subject_type VARCHAR(20) NOT NULL CHECK (subject_type IN ('USER', 'COLLABORATOR')),
    subject_id BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_subject ON refresh_tokens (subject_type, subject_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);

-- =========================================================================
-- TOKENS DE PUSH
--
-- Um colaborador pode ter vários aparelhos, e o mesmo token pode migrar de dono
-- se o aparelho for compartilhado — por isso a unicidade é do token, não do par
-- colaborador+token.
-- =========================================================================
CREATE TABLE device_tokens (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    collaborator_id BIGINT NOT NULL,
    token TEXT NOT NULL UNIQUE,
    platform VARCHAR(20) NOT NULL CHECK (platform IN ('ANDROID', 'IOS', 'WEB')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (collaborator_id) REFERENCES collaborators(id)
);

CREATE INDEX idx_device_tokens_collaborator ON device_tokens (collaborator_id) WHERE active;
