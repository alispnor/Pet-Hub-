# 🐾 Pet Hub — Prompt Inicial de Execução (Fase 0 + Fase 1)

> **Status:** [approved] — prompt pronto para colar no Claude Code e executar as duas primeiras fases em uma sessão.
> **Origem:** preparado pelo Ali para iniciar o desenvolvimento dentro de `~/projects/pet-hub/`.
> **Diferença para o `plano-completo.md`:** aquele é o plano estratégico das 12 fases; este é o prompt operacional para executar as Fases 0 e 1 juntas, com todos os detalhes técnicos prontos (entidades, endpoints, DTOs, migrations, padrão de código, checklist).

---

Você vai criar o projeto **Pet Hub** — um e-commerce completo de produtos tecnológicos para pets, com painel administrativo, emissão de NF-e, recomendações por IA e arquitetura cloud-native.

Este é um projeto de portfólio sênior. Cada decisão técnica deve ser justificada, o código deve ser production-ready e seguir boas práticas.

---

## 🎯 SOBRE O PROJETO

**Nome:** Pet Hub
**Tagline:** Your one-stop pet tech store
**Domínio:** Marketplace de produtos tech para pets (smart collars, GPS trackers, câmeras pet, comedouros IoT, fontes inteligentes, brinquedos interativos com IA)
**Ecossistema:** Faz parte do "Ali's Pet Ecosystem" junto com o Pet Diary (prontuário digital de pets, projeto irmão já existente)
**Autor:** Ali — Senior Full Stack Developer, Guep Technology, Santo André/SP, Brasil

---

## 🛠️ STACK TECNOLÓGICA

**Backend**
- Java 21 + Spring Boot 3.3+
- Maven multi-module
- PostgreSQL 16 + Flyway
- Spring Security 6 + JWT
- Lombok + MapStruct
- SpringDoc OpenAPI (Swagger)
- JUnit 5 + Testcontainers

**Frontend**
- Angular 17+ (Standalone Components)
- TypeScript strict
- TailwindCSS
- Angular Signals

**Infraestrutura**
- Docker + docker-compose
- Kubernetes
- Kafka + RabbitMQ
- Redis
- AWS ou Oracle Cloud

**IA**
- Anthropic Claude API
- Spring AI
- PGVector (RAG)

---

## 🎨 IDENTIDADE VISUAL

- **Primary:** #6366F1 (indigo)
- **Accent:** #F59E0B (amber)
- **Success:** #10B981 (emerald)
- **Danger:** #EF4444 (red)
- **Tipografia:** Inter (UI) + Poppins (display)
- **Logo:** pata estilizada dentro de hexágono

---

## 📋 ROADMAP EM 12 FASES

| Fase | Entrega |
|------|---------|
| 0 | Bootstrap do monorepo |
| 1 | Backend core: catálogo + autenticação |
| 2 | Cliente: pets, endereços, formas de pagamento |
| 3 | Carrinho, frete, checkout, pagamento |
| 4 | Pedidos, timeline, estoque |
| 5 | Frontend Angular (storefront) |
| 6 | Frontend Angular (admin/back-office) |
| 7 | Emissão de NF-e |
| 8 | Docker |
| 9 | Kafka + RabbitMQ |
| 10 | IA generativa (recomendações + chatbot) |
| 11 | Kubernetes |
| 12 | Cloud + CI/CD + Observabilidade |

---

## 🚀 TAREFA DESTA SESSÃO — FASE 0 + FASE 1

Vamos executar as duas primeiras fases nesta sessão.

---

### ═══════════════════════════════════
### FASE 0 — BOOTSTRAP DO MONOREPO
### ═══════════════════════════════════

Crie a estrutura inicial completa do monorepo dentro da pasta atual.

**Crie os seguintes arquivos:**

#### README.md
```markdown
# 🐾 Pet Hub

> Your one-stop pet tech store

Pet Hub é um marketplace de produtos tecnológicos para pets — smart collars, GPS trackers, câmeras pet, comedouros IoT e muito mais — com recomendações personalizadas por IA generativa.

Faz parte do **Ali's Pet Ecosystem** junto com o [Pet Diary](#) (prontuário digital de pets).

![Build](https://img.shields.io/badge/build-passing-brightgreen?style=flat-square)
![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-green?style=flat-square&logo=springboot)
![Angular](https://img.shields.io/badge/Angular-17-red?style=flat-square&logo=angular)
![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)

## ✨ Features

### Para o Cliente
- Catálogo completo de produtos pet tech com filtros e busca
- Cadastro com múltiplos pets, endereços e formas de pagamento
- Checkout completo: carrinho → endereço → frete → pagamento
- Pagamento via cartão, PIX e boleto
- Cálculo de frete em tempo real (Correios + transportadoras)
- Histórico de pedidos com timeline visual de cada etapa
- Recomendações personalizadas por IA baseadas no perfil do pet
- Chatbot assistente para dúvidas sobre produtos e cuidados

### Para o Administrador
- Dashboard com KPIs de vendas, estoque e pedidos
- CRUD de produtos com upload de imagens e specs técnicas
- Gestão de estoque com alertas de mínimo
- Controle de pedidos com transição de status
- Cupons e promoções por produto/categoria
- Relatórios de vendas exportáveis (Excel/PDF)
- Emissão de NF-e (modelo 55)
- Gestão de regras de imposto por NCM/UF
- Audit log de todas as ações administrativas

### Técnico
- Arquitetura event-driven com Kafka e RabbitMQ
- Containerizado com Docker, orquestrado com Kubernetes
- Deploy em cloud pública (AWS/Oracle)
- Observabilidade com Prometheus + Grafana + Loki

## 🛠️ Tech Stack

**Backend:** Java 21 · Spring Boot 3 · PostgreSQL · Kafka · RabbitMQ · Redis
**Frontend:** Angular 17 · TypeScript · TailwindCSS
**Infrastructure:** Docker · Kubernetes · AWS/Oracle Cloud
**AI:** Anthropic Claude API · Spring AI · PGVector

## 📋 Roadmap

Veja o [ROADMAP.md](./ROADMAP.md) — projeto em 12 fases progressivas.

## 🚀 Quick Start

```bash
# Clone o repositório
git clone https://github.com/alispnor/pet-hub.git
cd pet-hub

# Sobe o ambiente completo
make up

# Acessa:
# Storefront: http://localhost:4200
# Admin:      http://localhost:4201
# API:        http://localhost:8080/swagger-ui.html
```

## 📚 Documentação

- [ROADMAP.md](./ROADMAP.md) — Plano de desenvolvimento
- [ARCHITECTURE.md](./ARCHITECTURE.md) — Arquitetura do sistema
- [docs/brand/BRAND.md](./docs/brand/BRAND.md) — Identidade visual
- [CONTRIBUTING.md](./CONTRIBUTING.md) — Como contribuir

## 🌐 Ecossistema

```
Pet Diary (prontuário) ◄──── Ali's Pet Ecosystem ────► Pet Hub (e-commerce)
```

Futuramente os dois projetos vão integrar: o Pet Diary envia o perfil do pet para o Pet Hub gerar recomendações ultra-personalizadas.

## 👤 Autor

**Ali** — Senior Full Stack Developer
[GitHub](https://github.com/alispnor)

## 📄 Licença

MIT © 2026 Ali
```

#### ROADMAP.md
Crie um ROADMAP.md detalhado com as 12 fases. Para cada fase inclua:
- Objetivo da fase
- Lista de entregáveis técnicos
- Habilidades demonstradas
- Status: ⬜ Pendente (fases 1-12) / ✅ Concluída (fase 0 ao finalizar)
- Esforço estimado em horas

#### ARCHITECTURE.md
Esqueleto de arquitetura com:
- Visão geral do sistema
- Diagrama de contexto em ASCII (Pet Hub ↔ usuário, admin, Correios, gateway de pagamento, SEFAZ, IA)
- Lista de serviços/módulos planejados
- Padrões adotados: Clean Architecture, Event-Driven, CQRS (futuro)
- Estratégia evolutiva: monolito modular → microserviços

#### docs/brand/BRAND.md
Documentação de identidade visual:
- Cores com hex e nomes Tailwind
- Tipografia (Inter + Poppins)
- Tom de voz
- Iconografia (Lucide Icons)
- Conceito do logo

#### .gitignore (substitui o atual)
Completo para Java + Node + IDE + OS + Environment

#### .editorconfig
Padrão: Java=4 espaços, JS/TS/HTML/CSS/YAML=2 espaços

#### CONTRIBUTING.md
Com Conventional Commits, branch strategy, PR workflow

#### CHANGELOG.md
Formato Keep a Changelog, seção [Unreleased] inicial

#### Makefile
Com targets: help, build, test, up, down, clean, logs, ps
Use ## nos targets para aparecer no make help

#### .github/workflows/ci.yml
Placeholder com jobs: lint, test, build (com echo "TODO")
Trigger: push main + pull_request

#### .github/PULL_REQUEST_TEMPLATE.md
Com: descrição, tipo de mudança, como testar, checklist

#### .github/ISSUE_TEMPLATE/bug_report.md
Template completo de bug

#### .github/ISSUE_TEMPLATE/feature_request.md
Template de feature

#### docs/adr/0001-monorepo.md
ADR: Monorepo vs multi-repo → Monorepo

#### docs/adr/0002-arquitetura-evolutiva.md
ADR: Monolito modular → Microserviços na Fase 9

#### docs/adr/0003-pet-ecosystem.md
ADR: Pet Hub como parte do ecossistema Pet Diary + Pet Hub

#### Estrutura de pastas (criar com .gitkeep)
- backend/
- frontend/storefront/
- frontend/admin/
- infrastructure/docker/
- infrastructure/k8s/
- infrastructure/helm/
- infrastructure/terraform/
- docs/adr/
- docs/diagrams/
- docs/brand/

**Ao terminar a Fase 0, faça commits separados por escopo:**
```
git add README.md && git commit -m "docs: add project README with ecosystem context"
git add ROADMAP.md && git commit -m "docs: add 12-phase roadmap"
git add ARCHITECTURE.md docs/brand/ && git commit -m "docs: add architecture skeleton and brand guidelines"
git add docs/adr/ && git commit -m "docs: add ADRs for monorepo, architecture and ecosystem"
git add .gitignore .editorconfig CONTRIBUTING.md CHANGELOG.md && git commit -m "chore: add editor config and contributing guide"
git add Makefile && git commit -m "chore: add Makefile with dev targets"
git add backend/ frontend/ infrastructure/ docs/diagrams/ && git commit -m "chore: setup monorepo folder structure"
git add .github/ && git commit -m "ci: add GitHub Actions skeleton and issue templates"
```

---

### ═══════════════════════════════════
### FASE 1 — BACKEND CORE
### ═══════════════════════════════════

Agora crie o backend Spring Boot dentro de `backend/`.

#### ESTRUTURA MAVEN

```
backend/
├── pom.xml                  ← parent POM
├── common/                  ← utilitários, exceptions, ProblemDetail
│   └── pom.xml
├── identity/                ← autenticação e usuários
│   └── pom.xml
├── catalog/                 ← produtos e categorias
│   └── pom.xml
└── application/             ← Spring Boot Application entry point
    └── pom.xml
```

**Parent POM** (backend/pom.xml):
- groupId: com.alispnor.pethub
- artifactId: pet-hub-backend
- version: 1.0.0-SNAPSHOT
- packaging: pom
- modules: common, identity, catalog, application
- Dependências gerenciadas (dependencyManagement):
  * Spring Boot BOM 3.3.x
  * Lombok
  * MapStruct 1.5.x
  * jjwt 0.12.x (io.jsonwebtoken)
  * Bucket4j 8.x
  * logstash-logback-encoder 7.x
  * Testcontainers BOM
  * SpringDoc OpenAPI 2.x

**application/pom.xml** depende de: common, identity, catalog

#### CLEAN ARCHITECTURE (aplicar em identity e catalog)

Cada módulo segue:
```
src/main/java/com/alispnor/pethub/{modulo}/
├── domain/
│   ├── entity/        ← entidades JPA
│   └── event/         ← domain events (placeholder)
├── application/
│   ├── usecase/       ← services
│   ├── dto/           ← records: XxxRequest, XxxResponse
│   ├── mapper/        ← MapStruct interfaces
│   └── port/          ← interfaces Repository
└── infrastructure/
    ├── rest/          ← controllers
    └── persistence/   ← JPA repositories
```

#### MÓDULO COMMON

Classes a criar:
1. `BusinessException` — base de todas as exceptions de negócio
2. `ResourceNotFoundException extends BusinessException` — 404
3. `BusinessRuleException extends BusinessException` — 422
4. `ConflictException extends BusinessException` — 409
5. `GlobalExceptionHandler` (@RestControllerAdvice) — retorna RFC 7807 (Problem Details):
```json
{
  "type": "https://pethub.com/errors/not-found",
  "title": "Recurso não encontrado",
  "status": 404,
  "detail": "Produto não encontrado: COLLAR-001",
  "instance": "/api/v1/catalog/products/COLLAR-001",
  "timestamp": "2026-05-11T20:00:00Z"
}
```
6. `PageableResponse<T>` — wrapper padrão de paginação
7. `ValidCpf` — annotation de validação customizada
8. `ValidNcm` — annotation de validação (8 dígitos numéricos)

#### MÓDULO IDENTITY

**Entidades:**

```java
@Entity @Table(name = "usuarios")
class Usuario {
    Long id;
    String nome;          // not null, max 200
    String email;         // not null, unique, lowercase
    String senhaHash;     // BCrypt cost 12, NUNCA expor em DTO
    String cpf;           // unique, nullable, 11 dígitos sem máscara
    String telefone;      // nullable
    TipoUsuario tipo;     // enum: CLIENTE, ADMIN
    Set<Role> roles;      // @ElementCollection enum: ROLE_CLIENTE, ROLE_ADMIN_LOJA, ROLE_GERENTE, ROLE_OPERADOR
    boolean ativo;        // default true
    int tentativasFalhas; // default 0
    LocalDateTime bloqueadoAte; // nullable
    LocalDateTime dataCadastro;
    LocalDateTime ultimoLogin;
}

@Entity @Table(name = "refresh_tokens")
class RefreshToken {
    Long id;
    Usuario usuario;      // @ManyToOne
    String tokenHash;     // SHA-256 do token real, unique
    LocalDateTime expiraEm;
    boolean revogado;
    LocalDateTime criadoEm;
}
```

**Use cases (services):**

`AuthService`:
- `register(RegisterClienteRequest)` → UsuarioResponse
- `registerAdmin(RegisterAdminRequest, Usuario adminLogado)` → UsuarioResponse
- `login(LoginRequest, String ip)` → AuthResponse (com accessToken + refreshToken)
- `refresh(String refreshToken)` → AuthResponse (rotação de token)
- `logout(String refreshToken)` — revoga token
- `me(Long userId)` → UsuarioResponse

**DTOs:**
```java
record RegisterClienteRequest(
    @NotBlank String nome,
    @Email @NotBlank String email,
    @NotBlank @Size(min=8) String senha,
    String telefone
) {}

record LoginRequest(@Email @NotBlank String email, @NotBlank String senha) {}

record AuthResponse(
    String accessToken,
    String refreshToken,
    int expiresIn,
    UsuarioResponse usuario
) {}

record UsuarioResponse(Long id, String nome, String email, TipoUsuario tipo, Set<Role> roles) {}
```

**Security:**
- `JwtService` — gera, valida e extrai claims de JWT (jjwt 0.12.x)
- `JwtAuthenticationFilter` extends OncePerRequestFilter
- `SecurityConfig` @Configuration:
  - Rotas públicas: GET /api/v1/catalog/**, POST /api/v1/auth/register/**, POST /api/v1/auth/login, POST /api/v1/auth/refresh, /swagger-ui.html, /v3/api-docs, /actuator/health
  - Rotas admin: /api/v1/admin/** exige tipo=ADMIN no token
  - Todo o resto: autenticado

**Rate Limiting (Bucket4j):**
- Filter para /api/v1/auth/login: 5 req/min por IP
- Retorna 429 com header Retry-After

**Endpoints:**
```
POST /api/v1/auth/register/cliente  → 201
POST /api/v1/auth/register/admin    → 201 (ROLE_ADMIN_LOJA only)
POST /api/v1/auth/login             → 200 AuthResponse
POST /api/v1/auth/refresh           → 200 AuthResponse
POST /api/v1/auth/logout            → 204
GET  /api/v1/auth/me                → 200 UsuarioResponse
```

#### MÓDULO CATALOG

**Entidades:**

```java
@Entity @Table(name = "categorias")
class Categoria {
    Long id;
    String nome;           // max 100
    String slug;           // unique, kebab-case
    String descricao;      // max 500
    Categoria categoriaPai; // self-reference, nullable
    boolean ativo;
    int ordem;
    LocalDateTime criadoEm;
    LocalDateTime atualizadoEm;
}

@Entity @Table(name = "produtos")
class Produto {
    Long id;
    String sku;              // unique, max 50
    String nome;             // max 200
    String descricaoCurta;   // max 500
    String descricaoCompleta; // TEXT
    String marca;
    Categoria categoria;
    BigDecimal pesoKg;       // (8,3)
    BigDecimal alturaCm;     // (8,2)
    BigDecimal larguraCm;
    BigDecimal profundidadeCm;
    String ncm;              // 8 dígitos, obrigatório para NF-e
    Origem origem;           // enum: NACIONAL, IMPORTADO_DIRETO, IMPORTADO_INDIRETO
    Map<String,Object> specs; // JSONB via @Type(JsonType.class) ou @JdbcTypeCode(SqlTypes.JSON)
    boolean ativo;
    boolean destacado;
    LocalDateTime criadoEm;
    LocalDateTime atualizadoEm;
    List<ProdutoImagem> imagens; // @OneToMany
}

@Entity @Table(name = "produto_imagens")
class ProdutoImagem {
    Long id;
    Produto produto;
    String url;
    int ordem;
    boolean principal;
}

@Entity @Table(name = "precos_vigentes")
class PrecoVigente {
    Long id;
    Produto produto;
    BigDecimal valorBase;  // (10,2)
    LocalDateTime dataInicio;
    LocalDateTime dataFim; // null = vigente
    Usuario criadoPor;
    LocalDateTime criadoEm;
}
```

**Use cases:**

`CategoriaService`:
- `listarTodas(boolean ativas)` → List<CategoriaResponse> (hierárquica)
- `criarCategoria(CategoriaRequest, Usuario admin)` → CategoriaResponse
- `atualizarCategoria(Long id, CategoriaRequest)` → CategoriaResponse
- `desativarCategoria(Long id)` — soft delete

`ProdutoService`:
- `listar(ProdutoFiltro filtro, Pageable pageable)` → Page<ProdutoSummaryResponse>
- `buscarPorSku(String sku)` → ProdutoDetailResponse
- `buscar(String query, Pageable pageable)` → Page<ProdutoSummaryResponse>
- `criar(ProdutoRequest, Usuario admin)` → ProdutoDetailResponse
- `atualizar(Long id, ProdutoRequest)` → ProdutoDetailResponse
- `toggleAtivo(Long id)` — soft delete/restore
- `adicionarImagem(Long id, String url)` → ProdutoImagem
- `removerImagem(Long id, Long imgId)`
- `definirPreco(Long id, BigDecimal valor, Usuario admin)` → PrecoVigente

**DTOs (records):**
```java
record ProdutoSummaryResponse(
    Long id, String sku, String nome, String descricaoCurta,
    BigDecimal preco, String imagemPrincipal, String categoriaSlug
) {}

record ProdutoDetailResponse(
    Long id, String sku, String nome, String descricaoCurta,
    String descricaoCompleta, String marca, String ncm,
    BigDecimal preco, BigDecimal pesoKg,
    BigDecimal alturaCm, BigDecimal larguraCm, BigDecimal profundidadeCm,
    Map<String,Object> specs, List<ProdutoImagemResponse> imagens,
    CategoriaResponse categoria, boolean ativo, boolean destacado
) {}

record ProdutoRequest(
    @NotBlank String sku, @NotBlank String nome,
    String descricaoCurta, String descricaoCompleta, String marca,
    @NotNull Long categoriaId,
    @NotNull @DecimalMin("0.001") BigDecimal pesoKg,
    @NotBlank @ValidNcm String ncm,
    @NotNull Origem origem,
    Map<String,Object> specs,
    @NotNull @DecimalMin("0.01") BigDecimal precoInicial
) {}
```

**Endpoints:**
```
# Público (storefront)
GET  /api/v1/catalog/categories
GET  /api/v1/catalog/products
GET  /api/v1/catalog/products/{sku}
GET  /api/v1/catalog/products/search?q=

# Admin
POST   /api/v1/admin/catalog/categories
PUT    /api/v1/admin/catalog/categories/{id}
DELETE /api/v1/admin/catalog/categories/{id}
POST   /api/v1/admin/catalog/products
PUT    /api/v1/admin/catalog/products/{id}
PATCH  /api/v1/admin/catalog/products/{id}/ativo
POST   /api/v1/admin/catalog/products/{id}/images
DELETE /api/v1/admin/catalog/products/{id}/images/{imgId}
POST   /api/v1/admin/catalog/products/{id}/price
```

#### FLYWAY MIGRATIONS

**V1__schema_identity.sql** — tabelas: usuarios, usuario_roles, refresh_tokens (com índices)

**V2__schema_catalog.sql** — tabelas: categorias, produtos, produto_imagens, precos_vigentes (com índices)

**V3__seed_data.sql** — insere:
- Admin: admin@pethub.com / Admin@123 (hash BCrypt)
- Cliente teste: cliente@teste.com / Cliente@123 (hash BCrypt)
- 6 categorias: Smart Collars, Comedouros Automáticos, Câmeras Pet, GPS Trackers, Fontes Inteligentes, Brinquedos Interativos
- 20 produtos pet tech realistas com specs em JSON, NCM válido e imagens via picsum.photos
- Preços de R$ 89,90 a R$ 1.299,00

#### CONFIGURAÇÕES

**application/src/main/resources/application.yml**
**application/src/main/resources/application-dev.yml**
**application/src/main/resources/application-test.yml**

Configure todas as propriedades necessárias: datasource, JPA, Flyway, Security JWT, CORS, Actuator, SpringDoc, Logging (JSON via logstash).

Variáveis de ambiente usadas:
- `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`
- `JWT_SECRET` (mínimo 32 chars)
- `CORS_ALLOWED_ORIGINS`

#### TESTES

Para cada service: testes unitários com Mockito (happy path + exceptions)
Para cada controller: testes de integração com @SpringBootTest + Testcontainers PostgreSQL

Crie `BaseIntegrationTest` abstrata que:
- Sobe PostgreSQL via Testcontainers
- Configura datasource via @DynamicPropertySource
- Expõe MockMvc e ObjectMapper

Casos obrigatórios:
- Register e login OK
- Login com senha errada → 401
- Acessar /admin/** como CLIENTE → 403
- Token expirado → 401
- CRUD de produtos com admin autenticado
- Busca de produtos como público

#### PADRÃO DE CÓDIGO (OBRIGATÓRIO EM TODOS OS MÉTODOS)

```java
public ProdutoDetailResponse buscarPorSku(String sku) {
    log.debug("Buscando produto sku={}", sku);
    var produto = produtoRepository.findBySkuAndAtivoTrue(sku)
        .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado: " + sku));
    var response = produtoMapper.toDetailResponse(produto);
    log.debug("Produto encontrado id={}, nome={}", produto.getId(), produto.getNome());
    return response;
}
```

Regras:
- Todos os métodos seguem: log entrada → validação → lógica → log saída → retorno
- DTOs sempre como records Java
- Exceptions estendem BusinessException
- Lombok @Slf4j em todas as classes com log
- MapStruct para conversões (nunca conversão manual)
- Sem lógica de negócio em controllers (só delegam ao service)

#### DOCKER-COMPOSE para desenvolvimento local

Crie `infrastructure/docker/docker-compose.dev.yml`:
```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: pethub
      POSTGRES_USER: pethub
      POSTGRES_PASSWORD: pethub
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U pethub"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  pgadmin:
    image: dpage/pgadmin4:latest
    environment:
      PGADMIN_DEFAULT_EMAIL: admin@pethub.com
      PGADMIN_DEFAULT_PASSWORD: admin
    ports:
      - "5050:80"
    depends_on:
      postgres:
        condition: service_healthy

volumes:
  postgres-data:
```

#### README do backend

Crie `backend/README.md` com:
- Como rodar localmente
- Como rodar os testes
- Variáveis de ambiente necessárias
- Exemplos de chamadas curl/httpie para todos os endpoints
- Link para Swagger UI

---

### PADRÃO DE COMMITS DA FASE 1

```
git commit -m "chore(backend): setup Maven multi-module structure"
git commit -m "feat(identity): add Usuario and RefreshToken entities"
git commit -m "feat(identity): implement JWT auth with register and login"
git commit -m "feat(catalog): add Categoria and Produto entities"
git commit -m "feat(catalog): implement product CRUD endpoints"
git commit -m "feat(catalog): add price management"
git commit -m "db: add Flyway migrations V1, V2 and V3 with seed data"
git commit -m "test(identity): add unit and integration tests for auth"
git commit -m "test(catalog): add unit and integration tests for catalog"
git commit -m "docs(backend): add README with setup and API examples"
```

---

### CHECKLIST FINAL (verifique antes de encerrar)

- [ ] Parent POM com todos os módulos declarados
- [ ] Common com GlobalExceptionHandler retornando RFC 7807
- [ ] Entidades com relacionamentos corretos e anotações JPA
- [ ] JWT access token (15min) + refresh token com rotação (7 dias)
- [ ] Rate limit no endpoint de login (5/min/IP)
- [ ] Todos os endpoints implementados conforme especificado
- [ ] Migrations V1, V2, V3 com seed data
- [ ] application.yml / application-dev.yml / application-test.yml
- [ ] Testes unitários e de integração com Testcontainers
- [ ] docker-compose.dev.yml para rodar localmente
- [ ] Swagger funcionando em /swagger-ui.html
- [ ] Commits com Conventional Commits

Ao terminar, mostra:
1. `tree backend/ -L 4 -I 'target'`
2. `git log --oneline`
3. O Swagger JSON resumido (lista de endpoints criados)
