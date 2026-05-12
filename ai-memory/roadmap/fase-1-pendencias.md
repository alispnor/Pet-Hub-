# Fase 1 — Pendências e checklist final

> **Status:** Fase 1 entregue ~85%. Sessão pausada em 2026-05-11, retomada parcial em 2026-05-12.
> **Próxima sessão:** completar os itens abaixo, validar com `mvn clean install`, e só então marcar Fase 1 como ✅ no `ROADMAP.md`.

## 🔄 Progresso da retomada (2026-05-12)

Validação rodada com Maven em container (`maven:3.9-eclipse-temurin-21`) — máquina não tem JDK/Maven local. Comando usado:

```bash
docker run --rm -v "$PWD":/workspace -v /home/ali/.m2:/root/.m2 \
  -w /workspace maven:3.9-eclipse-temurin-21 \
  mvn -B -ntp clean install -DskipTests
```

**Resolvido:**
- ✅ `common` agora compila. Faltavam `spring-boot-starter-data-jpa` (para `org.springframework.data.domain.Page` em `PageableResponse`) e `spring-boot-starter-security` (para `BadCredentialsException`, `AuthenticationException`, `AccessDeniedException` em `GlobalExceptionHandler`). Adicionados em `backend/common/pom.xml` — **mudança não commitada ainda**.

**Próximo erro a resolver (onde a sessão parou):**
- ❌ Módulo `identity` falha ao resolver `com.bucket4j:bucket4j_jdk17-core:8.10.1` — artefato não existe no Maven Central com essas coordenadas. Coordenadas corretas precisam ser confirmadas: provavelmente `com.bucket4j:bucket4j-core` (sem `_jdk17`) na linha 8.x, OU mudar para versão mais antiga (7.x usava `com.github.vladimir-bukhtoyarov:bucket4j-core`). Atualizar `pom.xml` parent (linha ~104) e `identity/pom.xml`. Conferir em https://central.sonatype.com/artifact/com.bucket4j/bucket4j-core.

**Status do reactor após pausa:**
- Pet Hub Backend — SUCCESS
- Pet Hub :: Common — SUCCESS (após fix do pom)
- Pet Hub :: Identity — FAILURE (bucket4j)
- Pet Hub :: Catalog — SKIPPED
- Pet Hub :: Application — SKIPPED

Cache Maven em `/home/ali/.m2` foi populado parcialmente, então próxima sessão arranca mais rápido.

## ✅ O que foi entregue nesta sessão (10 commits)

1. `chore(backend): setup Maven multi-module structure`
2. `feat(common): add base exceptions, global handler and validators`
3. `feat(identity): add Usuario and RefreshToken entities`
4. `feat(identity): implement JWT auth with register, login and refresh`
5. `feat(catalog): add Categoria and Produto entities`
6. `feat(catalog): implement product and category CRUD endpoints`
7. `db: add Flyway migrations V0, V1, V2 and V3 with seed data`
8. `test: add integration tests with BaseIntegrationTest and Testcontainers`
9. `chore(docker): add docker-compose.dev.yml with postgres, redis and pgadmin`
10. `docs(backend): add README with setup, env vars and API examples`

## ⚠️ Pendências para concluir a Fase 1

### 1. Validar que o backend compila e sobe — **prioridade máxima**

Nada nesta sessão foi validado executando Maven/Java. **Tudo abaixo precisa rodar antes de declarar Fase 1 completa.**

```bash
# Subir infra
docker compose -f infrastructure/docker/docker-compose.dev.yml up -d

# Build limpo
cd backend
mvn clean install -DskipTests

# Rodar todos os testes (Testcontainers requer Docker)
mvn test

# Subir a aplicação
mvn -pl application spring-boot:run

# Validar manualmente alguns endpoints
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@pethub.com","senha":"Admin@123"}'

curl -s http://localhost:8080/api/v1/catalog/products?size=3
```

Provável que apareçam ajustes — itens de risco específicos abaixo.

### 2. Riscos conhecidos a verificar quando rodar

**a) Annotation processor do MapStruct.** O `pluginManagement` do parent POM declara o processor + Lombok + lombok-mapstruct-binding. Se a IDE/Maven não pegar a configuração, os mappers vão falhar em runtime (`UsuarioMapperImpl` não gerado). Se acontecer, verificar que cada módulo herda `maven-compiler-plugin` corretamente.

**b) `JsonType` do hypersistence-utils.** Em Hibernate 6.3+, o caminho do package mudou. O import usado foi `io.hypersistence.utils.hibernate.type.json.JsonType` — confirmar com a versão de Hibernate trazida pelo Spring Boot 3.3.5. Alternativa moderna usa `@JdbcTypeCode(SqlTypes.JSON)` sem dependência externa.

**c) `pg_trgm` no Testcontainer.** `V0__extensions.sql` cria a extensão; `postgres:16-alpine` inclui o contrib. Deve funcionar, mas se algum CI ambiente não tiver, fallback é remover o índice `gin_trgm_ops` em V2 (busca usa `ILIKE`, funciona sem trigram só com performance pior).

**d) `flyway-database-postgresql`.** Adicionado como dep separada porque Flyway 10+ exige isso em vez de só `flyway-core`. Versão é gerenciada pelo Spring Boot BOM (10.x). Se houver "no module found" para postgres, fixar a versão explicitamente.

**e) `spring-boot-maven-plugin` no `application` apenas.** O `<configuration><mainClass>` aponta para `com.alispnor.pethub.PetHubApplication`. Verificar que `mvn -pl application spring-boot:run` funciona.

**f) Bcrypt prefix `$2b$`.** Spring Security aceita `$2a`, `$2b`, `$2y`. Os hashes do seed foram gerados pela lib `bcrypt` do Python (prefixo `$2b$`). Se houver dúvida no `BCryptPasswordEncoder`, re-gerar com classe `BCryptPasswordEncoder` do Spring no startup e atualizar V3.

### 3. Testes que ainda faltam

A sessão entregou ~17 testes de integração cobrindo happy path + erros principais. Faltam:

- **Unit tests com Mockito** para os services (a spec pede "100% de cenários de erro" para `ProductService` e `AuthService`). Hoje só temos integration. Acrescentar:
  - `AuthServiceTest`: validateEmailDisponivel, bloqueio progressivo após 5 falhas, refresh com token revogado, refresh com token expirado.
  - `ProdutoServiceTest`: criar com SKU duplicado, atualizar inexistente, remover imagem inexistente, definirPreco fecha vigente anterior.
- **Integration tests faltando:**
  - Refresh token: fluxo OK + token revogado + token de outro usuário.
  - Logout: revoga o refresh; chamar refresh depois → 422/inválido.
  - Rate limit: 6 logins seguidos com mesma origem → 429 na 6ª.
  - Imagens: adicionar 2 imagens, marcar segunda como principal, primeira perde flag.
  - Preço: definir 2 preços; o anterior fica com `data_fim`; novo é vigente.
  - Conflict update: alterar SKU para um que já existe → 409.

Meta declarada no spec: **cobertura > 80% nos services**. Não medido nesta sessão (sem JaCoCo configurado).

### 4. Itens menores não entregues

- **JaCoCo plugin** no parent POM para gerar relatório de cobertura.
- **Spotless ou Checkstyle** para o `make lint` ficar real (hoje é placeholder).
- **`backend/common/README.md`, `backend/identity/README.md`, `backend/catalog/README.md`** — a spec do prompt-execucao pediu README por módulo. Foi entregue só `backend/README.md` agregando tudo. Aceitável, mas pode dividir.

### 5. Decisões a confirmar com o Ali

- **Rate limit in-memory.** `LoginRateLimitFilter` usa `ConcurrentHashMap` por IP — funciona em 1 instância. Quando houver >1 réplica (Fase 11), migrar para Bucket4j com backend Redis. Já anotado em comentário no código.
- **`flyway-database-postgresql` separado** — adicionei como dep em `application/pom.xml`. Se houver problema, comentar a dep e o `application.yml` já tem `spring.flyway.enabled: true` que basta para Flyway 9.x. Mas no Boot 3.3.5 vem Flyway 10.x onde é obrigatória.
- **CategoriaService.criar exige role GERENTE ou ADMIN_LOJA** — OPERADOR não pode criar categorias. Confirmar que está alinhado com a matriz de roles da spec.

### 6. Comandos rápidos para a próxima sessão

```bash
# Status do que ficou pendente
cat ai-memory/roadmap/fase-1-pendencias.md

# Subir tudo e rodar a primeira validação
docker compose -f infrastructure/docker/docker-compose.dev.yml up -d
cd backend && mvn clean install   # com testes; demora pela primeira vez (Testcontainers baixa imagem)

# Se quebrar, comum primeiro erro vai ser anotation processor ou JsonType.
```

## Onde retomar

Quando começar a próxima sessão, o agente deve:

1. Ler este arquivo primeiro.
2. Tentar `mvn clean install` no `backend/`.
3. Resolver o primeiro erro que aparecer — seguir a lista de "riscos conhecidos" como guia.
4. Depois de a build estar verde, completar testes faltantes da seção 3.
5. Quando estiver tudo passando, atualizar status no `ROADMAP.md` (Fase 1: 🚧 → ✅) e abrir a Fase 2.
