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

## 🎨 Design System

**MANDATORY:** All UI work in this project MUST follow [`ai-memory/design-system.md`](./ai-memory/design-system.md).
Before generating any component, screen, or style:
1. Read `ai-memory/design-system.md` in full
2. Use only tokens defined there (colors, spacing, typography)
3. Reject any UI suggestion that violates the anti-patterns (section 14)

## 🛠️ Tech Stack

**Backend:** Java 21 · Spring Boot 3 · PostgreSQL · Kafka · RabbitMQ · Redis
**Frontend:** Angular 17 · TypeScript · TailwindCSS
**Infrastructure:** Docker · Kubernetes · AWS/Oracle Cloud
**AI:** Anthropic Claude API · Spring AI · PGVector

## 📋 Roadmap

Veja o [ROADMAP.md](./ROADMAP.md) — projeto em 12 fases progressivas.

## 🚀 Rodando localmente (Fases 1–5)

> **Status atual:** Fases 1–4 ✅ entregues. Fase 5 (Storefront Angular) em andamento — slices 1 e 2 publicados. Veja [ROADMAP.md](./ROADMAP.md) e [`ai-memory/roadmap/`](./ai-memory/roadmap/) para o detalhamento.
>
> O **SPA de administração** entra só na Fase 6. Hoje a parte admin do produto é exposta como API REST sob `/api/v1/admin/...` (consumir via Swagger ou cURL — não há UI ainda).

### Pré-requisitos

- Docker + Docker Compose
- Node.js 18+ (para o storefront Angular)
- Arquivo `.env.local` na raiz do repo. Se não existir, copie do exemplo:
  ```bash
  cp .env.local.example .env.local
  ```
  Edite `APP_ENCRYPTION_KEY` (`openssl rand -base64 32`) e `JWT_SECRET` (`openssl rand -base64 64`) antes de subir.

### 1. Subir infra (Postgres + Redis + pgAdmin)

```bash
docker compose --env-file .env.local \
  -f infrastructure/docker/docker-compose.dev.yml up -d
```

| Serviço | URL |
|---|---|
| Postgres | `localhost:5432` (db/user/pwd do `.env.local`) |
| Redis | `localhost:6380` |
| pgAdmin | http://localhost:5050 |

### 2. Subir backend (porta 8080)

A máquina de dev não precisa de JDK/Maven nativos — o backend roda em container Maven com cache em `~/.m2`:

```bash
cd backend && docker run --rm -d --name pethub-app-dev \
  --env-file /home/ali/projects/pet-hub/.env.local \
  -v "$PWD":/workspace -v /home/ali/.m2:/root/.m2 \
  --network host -w /workspace \
  maven:3.9-eclipse-temurin-21 \
  mvn -B -ntp -pl application spring-boot:run -DskipTests
```

O Spring Boot leva ~15-30s pra subir na 1ª execução (download de deps). Acompanhe com `docker logs -f pethub-app-dev`.

| URL | O que é |
|---|---|
| http://localhost:8080/actuator/health | Healthcheck — deve responder `{"status":"UP"}` |
| http://localhost:8080/swagger-ui.html | **Swagger UI** — explorar/testar todos os endpoints |
| http://localhost:8080/v3/api-docs | OpenAPI JSON |

### 3. Subir storefront Angular (porta 4242)

```bash
cd frontend/storefront
npm install   # primeira vez apenas
npm start     # roda ng serve com host 127.0.0.1, port 4242 e proxy /api → :8080
```

Storefront em http://127.0.0.1:4242 (o proxy encaminha `/api/*` pra `http://localhost:8080`).

### Credenciais seed (dev)

Cobertura completa da matriz RBAC para smoke E2E manual e validação de permissões. Todas as senhas seguem o padrão "Tipo + @ + 123".

| Perfil | Email | Senha | Role |
|---|---|---|---|
| Admin Loja | `admin@pethub.com` | `Admin@123` | `ROLE_ADMIN_LOJA` |
| Gerente | `gerente@pethub.com` | `Gerente@123` | `ROLE_GERENTE` |
| Operador | `operador@pethub.com` | `Operador@123` | `ROLE_OPERADOR` |
| Cliente — Maria (Fase 2 seed) | `maria.fase2@pethub.com` | `Senha@123` | `ROLE_CLIENTE` |
| Cliente — Bruno (Fase 2 seed) | `bruno.fase2@pethub.com` | `Senha@123` | `ROLE_CLIENTE` |
| Cliente — Cliente Teste | `cliente@teste.com` | `Senha@123` | `ROLE_CLIENTE` |
| Cliente — João | `joao.teste@pethub.com` | `Teste@123` | `ROLE_CLIENTE` |

Os admins de teste (Gerente, Operador) e o cliente João vêm da migration `V16__test_users_extra.sql` — idempotente, então `docker compose down -v && up` os recria automaticamente. Novos cadastros podem ser feitos pelo `/cadastro` no storefront (só clientes) ou pelo `POST /api/v1/auth/register/cliente` no Swagger.

No Swagger, clicar em **Authorize** (canto superior direito) e colar o `accessToken` retornado por `POST /auth/login` (sem prefixo `Bearer`).

### Parar tudo

```bash
docker stop pethub-app-dev                                                      # backend
docker compose -f infrastructure/docker/docker-compose.dev.yml stop             # infra
# storefront: Ctrl+C no terminal do ng serve
```

### Troubleshooting rápido

- **Swagger devolve `connection refused`** → backend ainda subindo, espere 30s e tente `curl localhost:8080/actuator/health`.
- **Storefront carrega mas API dá CORS** → confira `CORS_ALLOWED_ORIGINS` no `.env.local` (deve incluir `http://127.0.0.1:4242`).
- **`port already in use`** → `docker ps` para ver containers antigos; `lsof -i :8080` ou `:4242` para processos locais.

## 📚 Documentação

- [ROADMAP.md](./ROADMAP.md) — Plano de desenvolvimento em 12 fases
- [ARCHITECTURE.md](./ARCHITECTURE.md) — Arquitetura do sistema
- [docs/brand/BRAND.md](./docs/brand/BRAND.md) — Identidade visual
- [CONTRIBUTING.md](./CONTRIBUTING.md) — Como contribuir
- [CHANGELOG.md](./CHANGELOG.md) — Histórico de mudanças
- [docs/adr/](./docs/adr/) — Architecture Decision Records

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
