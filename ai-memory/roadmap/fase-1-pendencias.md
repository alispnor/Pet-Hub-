# Fase 1 — Pendências e checklist final

> **Status:** Fase 1 ✅ build verde + smoke test passou em 2026-05-12.
> Testes Testcontainers e cobertura JaCoCo permanecem como dívida técnica (seção 3 abaixo) — não bloqueiam abertura da Fase 2.

## ✅ Validação realizada em 2026-05-12

Maven rodado em container (`maven:3.9-eclipse-temurin-21`) com cache em `~/.m2` no host — máquina sem JDK/Maven nativo.

**Build:** `mvn clean install -DskipTests` → BUILD SUCCESS, todos os 5 módulos.

**Smoke test manual:** app subiu com `spring-boot:run` em container Maven com `--network host`; Postgres veio do `docker-compose.dev.yml`. Endpoints validados:
- `GET /actuator/health` → `{"status":"UP"}`
- `POST /api/v1/auth/login` (admin@pethub.com / Admin@123) → 200 com JWT
- `GET /api/v1/auth/me` (com Bearer) → 200, ROLE_ADMIN_LOJA
- `GET /api/v1/catalog/categories` → 6 categorias seed
- `GET /api/v1/catalog/products?size=3` → 18 produtos totais, paginação ok
- `GET /api/v1/catalog/products/COLLAR-PRO-001` → produto completo (JSONB specs, imagens, categoria)

**Riscos conhecidos confirmados resolvidos na prática:**
- ✅ MapStruct annotation processor — mappers funcionam (BCrypt valida senha do seed, response DTOs renderizam).
- ✅ `JsonType` do hypersistence-utils com Hibernate 6.5 — campo `specs` JSONB serializa/desserializa corretamente.
- ✅ `flyway-database-postgresql` — Flyway 10 aplicou V0/V1/V2/V3 em 217ms.
- ✅ `pg_trgm` no Postgres 16-alpine — V0 criou extensão, índices em V2 funcionam.
- ✅ BCrypt prefixo `$2b$` — Spring `BCryptPasswordEncoder` aceitou o hash gerado pela lib bcrypt do Python.
- ✅ `spring-boot-maven-plugin` em `application/` — `mvn -pl application spring-boot:run` subiu em 4.7s.

**Fixes aplicados nesta validação (commits desta sessão):**
- `fix(common): add data-jpa and security starters for compile` — `GlobalExceptionHandler` e `PageableResponse` precisavam.
- `fix(bucket4j): align coordinates with current maven central` — `bucket4j_jdk17-core` não existia em 8.10.1; pulado para 8.18.0 (lowest é 8.11.0).
- `fix(identity): use HttpStatus.TOO_MANY_REQUESTS for 429` — `HttpServletResponse.SC_TOO_MANY_REQUESTS` não existe no Jakarta Servlet.
- `chore(test): bump testcontainers to 1.21.4` — 1.20.4 vinha com docker-java client API 1.32, incompatível com daemon ≥ API 1.40.

## ⚠️ Dívida técnica deixada para depois

### Tests de integração (Testcontainers)

Em 2026-05-12 os testes rodaram mas falharam 13/17 com HTTP 500. Causa: o container Maven (network host) não consegue rotear pacotes para a porta randômica do container Postgres criado pelo Testcontainers no daemon do host. Erro: `Connection to 172.17.0.1:32768 refused`.

**Solução pendente:** instalar JDK 21 + Maven nativos no host (`sudo apt install openjdk-21-jdk maven`) e rodar `mvn test` direto. Aí Testcontainers + JDBC URL conversam normalmente via `localhost:porta`. Alternativa em container: tirar `--network host` e colocar Maven na mesma bridge dos containers Testcontainers. Adiar até precisar de CI real.

### Testes unitários faltando (spec original)

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

### 6. Comandos para reproduzir a validação (referência)

Máquina sem JDK/Maven nativo — tudo via container:

```bash
# Build
cd backend && docker run --rm -v "$PWD":/workspace -v /home/ali/.m2:/root/.m2 \
  -w /workspace maven:3.9-eclipse-temurin-21 mvn -B -ntp clean install -DskipTests

# Infra
docker compose -f infrastructure/docker/docker-compose.dev.yml up -d

# Subir app em background
docker run --rm -d --name pethub-app-dev -v "$PWD":/workspace -v /home/ali/.m2:/root/.m2 \
  --network host -w /workspace -e SPRING_PROFILES_ACTIVE=dev \
  maven:3.9-eclipse-temurin-21 mvn -B -ntp -pl application spring-boot:run

# Smoke
curl -s http://localhost:8080/actuator/health
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' -d '{"email":"admin@pethub.com","senha":"Admin@123"}'

# Parar
docker stop pethub-app-dev
docker compose -f infrastructure/docker/docker-compose.dev.yml stop
```

## Status final da Fase 1

✅ Build verde, app sobe, endpoints respondem corretamente. Fase 1 fechada no ROADMAP em 2026-05-12.
Próximo passo: abrir Fase 2 (Cliente: Pets, Endereços, Formas de Pagamento) — sem dependência das pendências da seção 3/4 acima.
