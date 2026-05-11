# 🐾 Pet Hub — Backend

Backend monolítico modular em Spring Boot 3.3, Java 21.

## Estrutura

```
backend/
├── pom.xml             ← parent (Spring Boot BOM + deps gerenciadas)
├── common/             ← exceptions base, GlobalExceptionHandler (RFC 7807), validators
├── identity/           ← auth (JWT + BCrypt), usuários, roles, rate limit
├── catalog/            ← categorias, produtos, imagens, preços
└── application/        ← Spring Boot Application + configs + migrations + testes
```

Cada módulo segue Clean Architecture:

```
src/main/java/com/alispnor/pethub/{modulo}/
├── domain/         entidades JPA, enums, value objects
├── application/    use cases, DTOs (records), mappers (MapStruct)
└── infrastructure/ controllers REST, repositórios JPA, segurança
```

## Pré-requisitos

- Java 21
- Maven 3.9+
- Docker (para Postgres + Redis locais)

## Como rodar

### 1. Subir a infraestrutura local

```bash
docker compose -f infrastructure/docker/docker-compose.dev.yml up -d
```

Sobe:
- PostgreSQL 16 em `localhost:5432` (db: `pethub`, user/pass: `pethub`)
- Redis 7 em `localhost:6379`
- pgAdmin em `http://localhost:5050` (admin@pethub.com / admin)

### 2. Build + run

```bash
cd backend
mvn clean install -DskipTests
mvn -pl application spring-boot:run
```

Backend em `http://localhost:8080`.

### 3. Endpoints úteis

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Actuator health: <http://localhost:8080/actuator/health>

## Como rodar os testes

```bash
cd backend
mvn test
```

Testes de integração usam Testcontainers — um Postgres 16 efêmero sobe automaticamente. Requer Docker em execução.

## Variáveis de ambiente

Todas têm valores default para dev. Em produção, defina:

| Variável | Default dev | Descrição |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | Profile ativo |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/pethub` | JDBC URL |
| `DATABASE_USER` | `pethub` | Usuário do DB |
| `DATABASE_PASSWORD` | `pethub` | Senha do DB |
| `JWT_SECRET` | `dev-only-secret-please-override-...` | Segredo HS256 (mín. 32 bytes) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200,http://localhost:4201` | Origens permitidas (CSV) |

## Credenciais do seed

Migrations V1, V2 e V3 criam o schema e populam dados iniciais (definido em `application/src/main/resources/db/migration/`):

| Usuário | Senha | Tipo | Roles |
|---|---|---|---|
| `admin@pethub.com` | `Admin@123` | ADMIN | ROLE_ADMIN_LOJA |
| `cliente@teste.com` | `Cliente@123` | CLIENTE | ROLE_CLIENTE |

Além de 6 categorias e 18 produtos pet tech com NCM, specs jsonb e imagens.

## Exemplos de uso (curl / HTTPie)

### Login

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@pethub.com","senha":"Admin@123"}' | jq
```

Resposta:

```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "Yk0a...",
  "expiresIn": 900,
  "usuario": { "id": 1, "nome": "Admin Pet Hub", ... }
}
```

### Catálogo (público)

```bash
# Lista produtos
curl -s 'http://localhost:8080/api/v1/catalog/products?size=5' | jq

# Detalhe por SKU
curl -s http://localhost:8080/api/v1/catalog/products/COLLAR-PRO-001 | jq

# Filtra por categoria
curl -s 'http://localhost:8080/api/v1/catalog/products?categoria=smart-collars' | jq

# Busca
curl -s 'http://localhost:8080/api/v1/catalog/products/search?q=collar' | jq

# Lista categorias
curl -s http://localhost:8080/api/v1/catalog/categories | jq
```

### Criar produto (admin)

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@pethub.com","senha":"Admin@123"}' | jq -r .accessToken)

curl -s -X POST http://localhost:8080/api/v1/admin/catalog/products \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "sku": "NEW-PROD-001",
    "nome": "Novo Produto",
    "descricaoCurta": "Descrição curta",
    "marca": "MinhaMarca",
    "categoriaId": 1,
    "pesoKg": 0.500,
    "ncm": "85176259",
    "origem": "NACIONAL",
    "destacado": false,
    "precoInicial": 199.90
  }' | jq
```

### Refresh + Logout

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/refresh \
  -H 'Content-Type: application/json' \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}" | jq

curl -s -X POST http://localhost:8080/api/v1/auth/logout \
  -H 'Content-Type: application/json' \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}" -w "%{http_code}\n"
```

## Padrões de código

Aplicados em todo o módulo, conforme `ai-memory/.../feedback_coding_patterns.md`:

1. **Template de função** em todos os métodos públicos de service:
   ```java
   public Type metodo(Params p) {
       log.debug("Iniciando {} com params={}", "metodo", p);
       validar(p);
       var r = executarLogica(p);
       log.debug("Finalizado {} com resultado={}", "metodo", r);
       return r;
   }
   ```
2. **DTOs como records Java** (imutáveis).
3. **MapStruct** para todas as conversões entity ↔ DTO.
4. **Clean Architecture** por módulo (`domain/`, `application/`, `infrastructure/`).
5. **Exceptions** estendem `BusinessException` (common). Handler retorna RFC 7807.
6. **Sem lógica de negócio em controllers** — delegam ao service.
7. **Conventional Commits** com scope: `feat(catalog):`, `feat(identity):`, etc.

## Próximas fases

- **Fase 2:** módulo `customer` (perfil, pets, endereços, formas de pagamento com criptografia CPF + tokenização cartão).
- **Fase 3:** carrinho (Redis), pricing, shipping, payment, checkout.
- **Fase 4:** pedidos com máquina de estados e estoque com reservas.

Plano completo em [`../ai-memory/roadmap/plano-completo.md`](../ai-memory/roadmap/plano-completo.md).
