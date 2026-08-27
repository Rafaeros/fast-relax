-- Schema FastRelax: Arquitetura Multi-Tenant (SaaS)
-- Necessária para o EXCLUDE com restrição combinada (chair_id, session_date, tsrange)
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- =========================================================================
-- LOCALIZAÇÃO E EMPRESAS (TENANTS)
-- =========================================================================
CREATE TABLE states (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    acronym VARCHAR(2) NOT NULL UNIQUE,
    ibge_code VARCHAR(10) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE TABLE cities (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    state_id BIGINT NOT NULL REFERENCES states(id),
    name VARCHAR(255) NOT NULL,
    ibge_code VARCHAR(10) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE TABLE address (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    city_id BIGINT NOT NULL REFERENCES cities(id),
    cep VARCHAR(10) NOT NULL,
    street VARCHAR(255) NOT NULL,
    number VARCHAR(20) NOT NULL,
    complement VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE TABLE companies (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    address_id BIGINT NOT NULL REFERENCES address(id),
    cnpj VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    -- Rede em que as cadeiras desta empresa entram.
    --
    -- Fica aqui, e não em cada cadeira, porque é uma propriedade da planta:
    -- todas as cadeiras do cliente usam o mesmo SSID, e trocar a senha do
    -- Wi-Fi precisa ser uma edição só, não uma por equipamento.
    --
    -- A senha é AES-GCM como o CPF, e pelo mesmo motivo: é segredo de terceiro
    -- guardado por nós. Nunca sai da API em claro — só o ESP32 a recebe, no
    -- push da configuração.
    wifi_ssid VARCHAR(64),
    wifi_password_encrypted TEXT,
    wifi_updated_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- =========================================================================
-- CONFIGURAÇÃO DE SESSÕES (POR EMPRESA)
-- O singleton global vira uma relação 1:1 com a empresa.
-- =========================================================================
CREATE TABLE company_session_settings (
    company_id BIGINT PRIMARY KEY REFERENCES companies(id) ON DELETE CASCADE,
    default_duration_minutes INTEGER NOT NULL DEFAULT 5 CHECK (default_duration_minutes BETWEEN 1 AND 120),
    start_grace_minutes INTEGER NOT NULL DEFAULT 2 CHECK (start_grace_minutes BETWEEN 0 AND 60),
    early_start_minutes INTEGER NOT NULL DEFAULT 2 CHECK (early_start_minutes BETWEEN 0 AND 60),
    max_advance_days INTEGER NOT NULL DEFAULT 30 CHECK (max_advance_days BETWEEN 1 AND 365),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =========================================================================
-- USUÁRIOS (SISTEMA E PAINEL DA EMPRESA)
-- =========================================================================
CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_id BIGINT REFERENCES companies(id), -- NULL se for equipe Physical
    name VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL CHECK (role IN (
        'SYSADMIN',      -- Equipe Physical (Master)
        'COMPANY_ADMIN', -- Gestor do Cliente
        'COMPANY_RH'     -- RH do Cliente
    )),
    must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    -- Garante que clientes tenham empresa vinculada, e a Physical não.
    CONSTRAINT chk_users_company_role CHECK (
        (role = 'SYSADMIN' AND company_id IS NULL) OR
        (role IN ('COMPANY_ADMIN', 'COMPANY_RH') AND company_id IS NOT NULL)
    )
);

CREATE INDEX idx_users_company ON users (company_id) WHERE deleted_at IS NULL;

-- =========================================================================
-- DEPARTAMENTOS & COLABORADORES (ISOLADOS POR EMPRESA)
-- =========================================================================
CREATE TABLE departments (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    -- Nome do departamento só precisa ser único dentro da própria empresa
    CONSTRAINT uq_departments_company_name UNIQUE (company_id, name)
);

CREATE TABLE collaborators (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    department_id BIGINT NOT NULL REFERENCES departments(id),
    name VARCHAR(120) NOT NULL,
    cpf_encrypted TEXT NOT NULL,
    cpf_hash TEXT NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    -- Opcional: parte do quadro não tem e-mail corporativo, e exigir um travaria
    -- o cadastro de quem trabalha no chão de fábrica. Quem tem recupera a senha
    -- sozinho; quem não tem depende do RH redefinir.
    email VARCHAR(180),
    -- O CPF continua sendo o identificador (blind index), mas deixou de ser a
    -- credencial: agora existe senha própria, como no painel.
    password_hash VARCHAR(255) NOT NULL,
    -- Nasce com senha temporária entregue pelo RH; até trocá-la, o acesso fica
    -- restrito à própria troca.
    must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    -- CPF é único dentro da empresa, não globalmente: a mesma pessoa pode ser
    -- colaboradora de dois clientes diferentes.
    CONSTRAINT uq_collaborators_company_cpf UNIQUE (company_id, cpf_hash)
);

CREATE INDEX idx_collaborators_department ON collaborators (department_id) WHERE deleted_at IS NULL;

-- E-mail único dentro da empresa, e só entre os vigentes.
--
-- Parcial em duas frentes: linhas sem e-mail ficam de fora (NULL não colide, mas
-- não há por que carregá-las), e as removidas também — diferente do CPF, o
-- e-mail de quem saiu pode ser reaproveitado por outra pessoa, e é ele que
-- recebe o link de recuperação.
--
-- LOWER porque o índice é o que decide de quem é o e-mail na recuperação de
-- senha: sem isso, "Ana@x.com" e "ana@x.com" seriam contas diferentes.
CREATE UNIQUE INDEX uq_collaborators_company_email
ON collaborators (company_id, LOWER(email))
WHERE email IS NOT NULL AND deleted_at IS NULL;

-- Horários permitidos permanecem ligados diretamente ao colaborador (sem mudanças estruturais)
CREATE TABLE collaborator_work_schedules (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    collaborator_id BIGINT NOT NULL REFERENCES collaborators(id),
    day_of_week VARCHAR(10) NOT NULL CHECK (day_of_week IN ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY')),
    allowed_start_time TIME NOT NULL,
    allowed_end_time TIME NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);
CREATE UNIQUE INDEX uq_work_schedule_collaborator_day ON collaborator_work_schedules (collaborator_id, day_of_week) WHERE deleted_at IS NULL;

-- =========================================================================
-- GESTÃO DE FIRMWARES (PHYSICAL) E CADEIRAS (EMPRESAS)
-- =========================================================================
CREATE TABLE firmwares (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL,
    version VARCHAR(50) NOT NULL UNIQUE,
    release_notes TEXT,
    release_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE TABLE firmware_files (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    firmware_id BIGINT NOT NULL REFERENCES firmwares(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    -- SHA-256 em hexadecimal. É o que o ESP32 confere antes de gravar na flash:
    -- uma atualização corrompida no meio do caminho vira um dispositivo que não
    -- liga mais, e não há recuperação remota.
    file_hash VARCHAR(64) NOT NULL,
    -- Separa o que é gravável do que é só arquivo: o esptool trabalha com imagem
    -- binária (.bin); Intel HEX (.hex) é formato de AVR e não pode ser enviado
    -- ao ESP32 como está.
    content_type VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- O mesmo nome duas vezes na mesma versão seria ambíguo na hora de gravar.
    CONSTRAINT uq_firmware_files_name UNIQUE (firmware_id, file_name)
);

-- Bytes do binário, em tabela própria.
--
-- O Hibernate lista todas as colunas mapeadas em cada SELECT: com o bytea junto
-- de firmware_files, abrir o catálogo carregaria megabytes por linha para montar
-- uma tabela que só mostra nome e versão. Separado, o conteúdo só é lido por
-- quem pede o download.
--
-- A chave é o próprio id do arquivo, não uma sequência nova: a linha não existe
-- sem ele, e o CASCADE faz o conteúdo sumir junto.
CREATE TABLE firmware_file_contents (
    firmware_file_id BIGINT PRIMARY KEY REFERENCES firmware_files(id) ON DELETE CASCADE,
    content BYTEA NOT NULL
);

CREATE TABLE chairs (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    firmware_id BIGINT REFERENCES firmwares(id),
    name VARCHAR(100) NOT NULL,
    mac_address VARCHAR(17) NOT NULL UNIQUE,
    ip_address VARCHAR(45),
    port INTEGER NOT NULL DEFAULT 80 CHECK (port BETWEEN 1 AND 65535),
    last_seen_at TIMESTAMP,
    cooldown_until TIMESTAMP,
    -- Ponto de acesso em que esta cadeira deve entrar, dentro do SSID da
    -- empresa. Fica por cadeira, e não por empresa, porque cada uma está
    -- fisicamente perto de um AP diferente: um BSSID único na empresa
    -- empurraria todas para o mesmo ponto, inclusive as do outro galpão.
    --
    -- Nulo deixa o ESP32 escolher o AP de melhor sinal.
    wifi_bssid VARCHAR(17),
    -- Quando o ESP32 confirmou que gravou a configuração na NVS dele.
    --
    -- Só isso separa "o SYSADMIN preencheu o formulário" de "a cadeira está de
    -- fato com a rede certa" — e a diferença entre os dois é uma cadeira que
    -- não volta depois que o Wi-Fi da empresa muda.
    network_synced_at TIMESTAMP,
    -- SSID que a cadeira relatou no último heartbeat. É a confirmação de que a
    -- configuração enviada foi aplicada, e não só recebida.
    reported_ssid VARCHAR(64),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_chairs_company ON chairs (company_id) WHERE deleted_at IS NULL;

-- =========================================================================
-- SESSÕES DE DESCANSO
-- =========================================================================
CREATE TABLE collaborator_sessions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    collaborator_id BIGINT NOT NULL REFERENCES collaborators(id),
    chair_id BIGINT REFERENCES chairs(id),
    session_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('SCHEDULED','STARTED','DONE','EXPIRED','CANCELLED')),
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_collaborator_active_session ON collaborator_sessions (collaborator_id) WHERE status IN ('SCHEDULED', 'STARTED');

-- NOVA EXCLUSÃO: Agora que uma empresa pode ter VÁRIAS cadeiras, sessões simultâneas 
-- SÃO PERMITIDAS, desde que não ocorram na MESMA cadeira.
-- O GIST aceita escalar (chair_id WITH =) graças à extensão btree_gist já declarada.
ALTER TABLE collaborator_sessions
    ADD CONSTRAINT uq_session_no_overlap
    EXCLUDE USING gist (
        chair_id WITH =,
        session_date WITH =,
        tsrange(
            (session_date + start_time)::timestamp,
            (session_date + end_time)::timestamp
        ) WITH &&
    )
    WHERE (status IN ('SCHEDULED', 'STARTED') AND chair_id IS NOT NULL);
-- =========================================================================
-- REFRESH TOKENS
--
-- Persistidos em vez de JWT autocontido: só assim é possível revogar o acesso
-- antes do vencimento. Guardamos o hash SHA-256, nunca o token em claro — um
-- vazamento do banco não pode ser usado para autenticar.
--
-- Sem company_id: o vínculo com a empresa vem do sujeito (user ou colaborador),
-- e duplicá-lo aqui criaria uma segunda fonte de verdade capaz de divergir.
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
-- DESTINOS DE PUSH
--
-- FCM (Android/iOS) entrega por um token opaco. Web Push entrega por uma
-- subscription — endpoint do serviço do navegador mais as chaves de
-- criptografia. Formatos diferentes demais para a mesma coluna: cada plataforma
-- preenche o campo que lhe cabe e o CHECK garante que um dos dois exista.
--
-- company_id é redundante em relação a collaborators.company_id, mas evita join
-- em toda listagem e deixa o filtro de tenant idêntico ao das demais tabelas.
-- =========================================================================
CREATE TABLE device_tokens (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    collaborator_id BIGINT NOT NULL REFERENCES collaborators(id),
    token TEXT,
    push_subscription JSONB,
    platform VARCHAR(20) NOT NULL CHECK (platform IN ('ANDROID', 'IOS', 'WEB')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_device_tokens_destination CHECK (
        (platform = 'WEB' AND push_subscription IS NOT NULL)
        OR (platform IN ('ANDROID', 'IOS') AND token IS NOT NULL)
    )
);

CREATE INDEX idx_device_tokens_collaborator ON device_tokens (collaborator_id) WHERE active;

-- Índice parcial em vez de UNIQUE de coluna: com token nulo nas linhas WEB o
-- unique comum ainda funcionaria (NULLs não colidem), mas carregaria essas
-- linhas à toa. O token do FCM é global — o mesmo aparelho não pode estar
-- registrado em duas empresas ao mesmo tempo.
CREATE UNIQUE INDEX uq_device_tokens_token
ON device_tokens (token)
WHERE token IS NOT NULL;

-- O endpoint identifica a inscrição do navegador do mesmo jeito que o token
-- identifica o aparelho: reinscrever atualiza a linha existente em vez de criar
-- outra. Índice por expressão evita uma coluna redundante.
CREATE UNIQUE INDEX uq_device_tokens_endpoint
ON device_tokens ((push_subscription ->> 'endpoint'))
WHERE push_subscription IS NOT NULL;

-- =========================================================================
-- NOTIFICAÇÕES: o que foi avisado, independente da entrega
--
-- Push é entrega best-effort: aparelho desligado, permissão revogada ou
-- navegador fechado fazem a mensagem se perder sem aviso. Persistir aqui dá ao
-- app uma central de notificações que não depende de o push ter chegado.
-- =========================================================================
CREATE TABLE notifications (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    collaborator_id BIGINT NOT NULL REFERENCES collaborators(id),
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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- A listagem é sempre "as minhas, mais recentes primeiro".
CREATE INDEX idx_notifications_collaborator
ON notifications (collaborator_id, created_at DESC);

-- O contador do sininho só olha as não lidas.
CREATE INDEX idx_notifications_unread
ON notifications (collaborator_id)
WHERE read_at IS NULL;

-- =========================================================================
-- MARCAS DE LEMBRETE POR SESSÃO
--
-- Um carimbo só na sessão bastaria para um lembrete e falharia para três:
-- enviar o de 1 hora marcaria a sessão como lembrada e engoliria o de 5 minutos.
-- Uma linha por (sessão, faixa) deixa as faixas independentes, e acrescentar uma
-- nova amanhã é mudar a configuração, não migrar tabela.
-- =========================================================================
CREATE TABLE session_reminders (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- A sessão some, as marcas somem junto: sem a sessão elas não significam nada.
    session_id BIGINT NOT NULL REFERENCES collaborator_sessions(id) ON DELETE CASCADE,
    -- 'DAY_BEFORE' para o resumo diário; 'T-60', 'T-5' para os rolantes. O
    -- formato carrega o offset, então a faixa nova não exige nada do banco.
    kind VARCHAR(20) NOT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Barreira de verdade contra lembrete repetido: se duas execuções se cruzarem,
-- a segunda esbarra aqui em vez de mandar push duplicado.
CREATE UNIQUE INDEX uq_session_reminders_session_kind
ON session_reminders (session_id, kind);

-- =========================================================================
-- TOKENS DE CREDENCIAL (convite de primeiro acesso e recuperação de senha)
--
-- Mesma linha de raciocínio dos refresh tokens: o valor sorteado vai por e-mail
-- e o banco guarda só o SHA-256. Um vazamento desta tabela não permite definir
-- a senha de ninguém.
--
-- Uso único e com validade curta, porque o link circula por um canal que não
-- controlamos — caixa de entrada encaminhada, histórico de navegador, print em
-- grupo de WhatsApp.
--
-- Sem FK para users nem collaborators: o par (subject_type, subject_id) aponta
-- para uma das duas tabelas, como em refresh_tokens. A alternativa seriam duas
-- colunas nulas e um CHECK garantindo exatamente uma preenchida — mais peso
-- para o mesmo resultado.
-- =========================================================================
CREATE TABLE credential_tokens (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    token_hash TEXT NOT NULL UNIQUE,
    -- INVITE: conta recém-criada, ainda sem senha utilizável.
    -- RESET:  a pessoa pediu recuperação e já tinha senha.
    purpose VARCHAR(20) NOT NULL CHECK (purpose IN ('INVITE', 'RESET')),
    subject_type VARCHAR(20) NOT NULL CHECK (subject_type IN ('USER', 'COLLABORATOR')),
    subject_id BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Emitir um token novo invalida os anteriores daquela pessoa; o índice é o que
-- torna essa varredura barata.
CREATE INDEX idx_credential_tokens_subject ON credential_tokens (subject_type, subject_id);
CREATE INDEX idx_credential_tokens_expires_at ON credential_tokens (expires_at);
