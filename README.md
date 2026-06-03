# FastRelax API

REST API para agendamento de sessões de relaxamento de colaboradores. Permite que o RH gerencie departamentos, colaboradores, horários de trabalho e sessões de massagem/relaxamento.

---

## Tecnologias

- **Java 21** + **Spring Boot 4.0.6**
- **PostgreSQL** — banco de dados relacional
- **Spring Security** + **JWT** — autenticação stateless
- **Spring Data JPA** + **Hibernate** — ORM
- **Flyway** — migrations de banco (configurado, mas DDL via Hibernate por padrão)
- **Lombok** — redução de boilerplate
- **Maven** — build

---

## Pré-requisitos

- Java 21+
- PostgreSQL rodando localmente (ou via Docker)
- Maven 3.9+ (ou use o wrapper `mvnw`)

---

## Configuração

### 1. Variáveis de ambiente

Copie `.env.example` para `.env` dentro de `fastrelax-api/` e preencha:

```env
DB_URL=jdbc:postgresql://localhost:5432/<nome_do_banco>
DB_USER=<usuario_postgres>
DB_PASS=<senha_postgres>
JWT_SECRET=<chave_sha256_para_jwt>
AES_SECRET=<chave_aes_para_criptografia>
```

### 2. Banco de dados

Crie o banco no PostgreSQL:

```sql
CREATE DATABASE fastrelax;
```

O Hibernate cria as tabelas automaticamente no primeiro boot (`ddl-auto=update`).

### 3. Rodar a aplicação

```bash
# Windows
cd fastrelax-api
mvnw.cmd spring-boot:run

# Linux/Mac
cd fastrelax-api
./mvnw spring-boot:run
```

API disponível em: `http://localhost:8090/api/v1`

---

## Usuário administrador padrão

Criado automaticamente no primeiro boot:

| Campo | Valor |
|-------|-------|
| Email | `admin@fastrelax.com` |
| Senha | `admin123` |

> Troque a senha em produção.

---

## Autenticação

Todas as rotas (exceto login) requerem JWT no header:

```
Authorization: Bearer <token>
```

### Login

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "admin@fastrelax.com",
  "password": "admin123"
}
```

**Resposta:**

```json
{
  "token": "<jwt_token>"
}
```

Token válido por **2 horas** (fuso UTC-3).

---

## Endpoints

### Usuários `[ADMIN]`

| Método | Rota | Descrição |
|--------|------|-----------|
| `POST` | `/api/v1/users` | Criar usuário |
| `GET` | `/api/v1/users` | Listar usuários (paginado, 10/página) |
| `GET` | `/api/v1/users/{id}` | Buscar usuário por ID |

#### Criar usuário

```http
POST /api/v1/users
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Maria Silva",
  "email": "maria@empresa.com",
  "password": "senha123",
  "role": "RH"
}
```

**Roles disponíveis:** `ADMIN` | `RH`

---

## Estrutura do projeto

```
fastrelax-api/
└── src/main/java/br/rafaeros/fastrelax_api/
    ├── FastrelaxApiApplication.java     # Entry point
    ├── core/
    │   ├── config/AdminSeeder.java      # Seed do admin inicial
    │   ├── dto/ApiResponseDTO.java      # Wrapper padrão de resposta
    │   ├── exceptions/                  # GlobalExceptionHandler + exceções de negócio
    │   └── security/                    # JWT filter, TokenService, SecurityConfig
    └── features/
        ├── auth/                        # Login endpoint
        ├── users/                       # CRUD de usuários
        └── collaborators/               # [Em desenvolvimento]
```

---

## Schema do banco

| Tabela | Descrição |
|--------|-----------|
| `users` | Usuários do sistema (ADMIN / RH) |
| `departments` | Departamentos da empresa |
| `collaborators` | Colaboradores (CPF criptografado) |
| `collaborator_work_schedules` | Horários de almoço por dia da semana |
| `collaborator_sessions` | Sessões de relaxamento agendadas |

### Status de sessão

```
SCHEDULED → IN_PROGRESS → DONE
                        → EXPIRED
                        → CANCELLED
```

Regra: apenas uma sessão ativa (`SCHEDULED` ou `IN_PROGRESS`) por colaborador por vez.

---

## Roadmap

- [x] Autenticação JWT com controle de roles
- [x] CRUD de usuários (ADMIN / RH)
- [ ] CRUD de departamentos
- [ ] CRUD de colaboradores (com CPF criptografado via AES)
- [ ] Gerenciamento de horários de trabalho
- [ ] Agendamento e gestão de sessões de relaxamento
- [ ] Expiração automática de sessões

---

## Padrão de resposta da API

```json
{
  "success": true,
  "data": { },
  "message": "Operação realizada com sucesso!",
  "timestamp": "2026-06-03T10:00:00"
}
```

Erros retornam `success: false` com `message` descritiva e HTTP status apropriado.

---

## Testes

```bash
cd fastrelax-api
mvnw.cmd test
```

---

## Configurações relevantes

| Propriedade | Valor |
|-------------|-------|
| Porta | `8090` |
| Context path | `/api/v1` |
| DDL auto | `update` |
| JWT expiration | 2 horas |
| Paginação padrão | 10 itens/página |
