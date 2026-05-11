# 🐾 Pet-Hub Store — Plano de Ação V1 (Portfolio, 9 fases)

> **Status:** [superseded] por `plano-completo.md` (V2, 12 fases).
> **Origem:** versão inicial do plano, focada em portfolio profissional / habilidades para vaga Senior Full Stack.
> **Por que mantido:** referência das decisões e do framing original (checklist de skills, escopo enxuto). A V2 expande para e-commerce completo (checkout, NF-e, back-office); esta V1 fica como histórico do escopo MVP/portfolio.

> Projeto portfolio para demonstrar todas as habilidades técnicas exigidas em vagas Senior Full Stack.

---

## 📦 Visão Geral

**Pet-Hub Store** é um marketplace de produtos tecnológicos para pets (coleiras inteligentes GPS, comedouros automáticos, câmeras pet, fontes de água inteligentes, brinquedos interativos com IA, etc), com recomendações personalizadas via IA generativa baseadas no perfil do pet do usuário.

### Habilidades Demonstradas (checklist)

- [x] Java + Spring Boot
- [x] Angular + HTML/CSS/JavaScript
- [x] SQL e banco relacional (PostgreSQL/MySQL/Oracle)
- [x] APIs REST (criar, consumir, manter)
- [x] Docker (imagens e containers)
- [x] Kubernetes (pods, deployments, services, ingresses)
- [x] Cloud pública (AWS/Oracle/Azure/GCP)
- [x] Mensageria (Kafka **e** RabbitMQ)
- [x] IA Generativa (Claude/OpenAI API)

---

## 🗂️ Estrutura do Repositório

```
pet-hub-store/
├── backend/
│   ├── catalog-service/       # Catálogo de produtos pet tech
│   ├── order-service/         # Gestão de pedidos
│   ├── recommendation-service/# IA + recomendações
│   ├── notification-service/  # Email/push (RabbitMQ)
│   └── api-gateway/           # Spring Cloud Gateway
├── frontend/                  # Angular 17+
├── infrastructure/
│   ├── docker/                # docker-compose, Dockerfiles
│   ├── k8s/                   # Manifests Kubernetes
│   ├── helm/                  # Helm charts
│   └── terraform/             # IaC para cloud
├── docs/                      # Diagramas C4, ADRs
└── .github/workflows/         # CI/CD
```

---

# 🚀 FASES E PROMPTS

---

## FASE 1 — Backend Core: Catálogo + Auth (Spring Boot Monolito)

### 🎯 Objetivo
Estabelecer a base com Spring Boot 3, demonstrando domínio de Java, JPA, REST, SQL.

### ✅ Entregáveis
- API REST de produtos, categorias, usuários
- Autenticação JWT
- PostgreSQL + Flyway
- Testes (JUnit + Testcontainers)
- OpenAPI/Swagger
- Tratamento global de exceções

### 📝 Prompt para Claude Code

```xml
<role>
Você é um arquiteto Java Senior especialista em Spring Boot 3, com foco em código limpo, SOLID, e Clean Architecture. Você produz código production-ready, com testes e documentação.
</role>

<projeto>
Nome: Pet-Hub Store - Backend Monolito (Fase 1)
Domínio: Marketplace de produtos tecnológicos para pets (smart collars, GPS trackers, comedouros automáticos, câmeras pet, etc).
</projeto>

<stack>
- Java 21 (use record onde fizer sentido, sealed classes, pattern matching)
- Spring Boot 3.3+
- Spring Web, Spring Data JPA, Spring Security
- PostgreSQL 16
- Flyway para migrations
- Lombok + MapStruct
- SpringDoc OpenAPI 2.x (Swagger UI)
- JUnit 5 + Mockito + AssertJ + Testcontainers
- Maven (multi-module ready, mas começe single-module)
</stack>

<arquitetura>
Use Clean Architecture / Hexagonal:
- domain/         → entidades, value objects, regras de negócio puras
- application/    → use cases (services), DTOs, ports
- infrastructure/ → adapters: REST controllers, JPA repositories, security
- config/         → configurações Spring

Padrões obrigatórios:
- DTOs separados de entidades (use MapStruct)
- Repository pattern via Spring Data
- Exception handler global com @RestControllerAdvice
- Validações com Bean Validation (jakarta.validation)
- Senhas com BCrypt
- JWT com expiração configurável via application.yml
</arquitetura>

<modelo_dominio>
Entidades principais:
1. **Usuario**: id, nome, email (unique), senha (hash), role (USER/ADMIN), pets (1:N)
2. **Pet**: id, nome, especie (CACHORRO/GATO/AVE/OUTROS), raca, idade, peso, usuario_id
3. **Categoria**: id, nome, descricao, slug (ex: "smart-collars", "comedouros-automaticos")
4. **Produto**: id, nome, descricao, preco, estoque, categoria_id, imagens (lista), specs (JSON), ativo
5. **Avaliacao**: id, produto_id, usuario_id, nota (1-5), comentario, criado_em

Use enums Java para especie e role.
specs do produto deve ser tipo jsonb no Postgres, mapeado como Map<String, Object>.
</modelo_dominio>

<endpoints_obrigatorios>
**Auth:**
- POST /api/auth/register
- POST /api/auth/login → retorna JWT
- GET  /api/auth/me (autenticado)

**Produtos (públicos para listagem, ADMIN para escrita):**
- GET    /api/products?categoria={slug}&page=0&size=20&sort=preco,asc
- GET    /api/products/{id}
- POST   /api/products (ADMIN)
- PUT    /api/products/{id} (ADMIN)
- DELETE /api/products/{id} (ADMIN)

**Categorias:**
- GET /api/categories
- POST /api/categories (ADMIN)

**Pets (usuário autenticado gerencia os seus):**
- GET    /api/pets
- POST   /api/pets
- PUT    /api/pets/{id}
- DELETE /api/pets/{id}

**Avaliações:**
- GET  /api/products/{id}/reviews
- POST /api/products/{id}/reviews (autenticado)
</endpoints_obrigatorios>

<padroes_de_codigo>
Ao escrever funções dentro de uma classe, todas devem seguir a mesma estrutura template:
1. Log de entrada (nível DEBUG)
2. Validações
3. Lógica principal
4. Log de saída
5. Retorno

Exemplo:
```java
public ProductResponse findById(Long id) {
    log.debug("Buscando produto id={}", id);
    var product = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado: " + id));
    var response = mapper.toResponse(product);
    log.debug("Produto encontrado: {}", response.nome());
    return response;
}
```

Use SLF4J via Lombok @Slf4j.
DTOs como records.
Use Optional apenas em retornos de repositórios.
</padroes_de_codigo>

<dados_iniciais>
Crie migration Flyway V1__schema.sql + V2__seed_data.sql com:
- 5 categorias: Smart Collars, Comedouros Automáticos, Câmeras Pet, GPS Trackers, Fontes Inteligentes
- 15 produtos pet tech reais/fictícios bem descritos
- 1 usuário ADMIN (admin@pethub.com / senha hash bcrypt de "admin123")
</dados_iniciais>

<testes>
Cobertura mínima:
- ProductService: 100% de casos felizes + exceções
- AuthService: register, login, token inválido, usuário duplicado
- ProductController: testes de integração com @SpringBootTest + Testcontainers PostgreSQL
- Pelo menos 1 teste de integração end-to-end por endpoint público
</testes>

<entregaveis>
1. Estrutura completa de pastas
2. pom.xml com todas as dependências
3. application.yml + application-dev.yml + application-test.yml
4. Migrations Flyway
5. Todas as classes (entities, repos, services, controllers, DTOs, mappers, configs, security)
6. Testes
7. README.md com: como rodar local, como rodar testes, exemplos curl/httpie dos endpoints, link do Swagger
8. .gitignore Java apropriado
</entregaveis>

<processo>
Antes de gerar qualquer código, raciocine passo a passo:
1. Liste a ordem em que vai criar os arquivos (dependências primeiro)
2. Identifique pontos de atenção (segurança JWT, transações, lazy loading)
3. Só então comece a criar

Ao final, faça uma auto-revisão: o código está aderente a Clean Architecture? Há lazy loading exposto via controller? Senhas estão hashed? Testes cobrem casos de erro?
</processo>
```

---

## FASE 2 — Frontend Angular

### 🎯 Objetivo
Construir SPA Angular consumindo a API da Fase 1, demonstrando Angular moderno + HTML/CSS/JS.

### ✅ Entregáveis
- Angular 17+ standalone components
- Páginas: home, catálogo, detalhe produto, login/cadastro, carrinho, meus pets, perfil
- Autenticação JWT + Guards + Interceptors
- Design responsivo (mobile-first)
- Lazy loading

### 📝 Prompt para Claude Code

```xml
<role>
Você é um Frontend Engineer Senior especialista em Angular 17+, com olhar de UX/UI. Produz código TypeScript estrito, componentes reutilizáveis, SCSS organizado, e SPAs com excelente performance.
</role>

<projeto>
Frontend Angular para o Pet-Hub Store (consome a API REST do backend da Fase 1).
URL da API: configurável via environment.ts (default http://localhost:8080).
</projeto>

<stack>
- Angular 17+ com Standalone Components (sem NgModules)
- TypeScript em modo strict
- Angular Signals + RxJS onde necessário
- Angular Router com lazy loading
- HttpClient + Interceptors
- Reactive Forms
- SCSS modular (variáveis, mixins, design tokens)
- Angular Material OU TailwindCSS (escolha um e justifique no README)
- ESLint + Prettier
</stack>

<estrutura>
src/app/
├── core/
│   ├── guards/         (authGuard, adminGuard)
│   ├── interceptors/   (authInterceptor, errorInterceptor)
│   ├── services/       (auth, api, toast)
│   └── models/         (interfaces TypeScript dos DTOs)
├── shared/
│   ├── components/     (button, card, modal, loading, navbar, footer)
│   └── pipes/          (currency BRL, etc)
├── features/
│   ├── home/
│   ├── catalog/
│   ├── product-detail/
│   ├── cart/
│   ├── auth/           (login, register)
│   ├── my-pets/
│   └── profile/
└── app.routes.ts
</estrutura>

<paginas_e_funcionalidades>
1. **Home (/):** Hero com banner pet tech, destaques de categorias, produtos em destaque
2. **Catálogo (/produtos):** Listagem com filtros (categoria, faixa de preço), paginação, ordenação. Sidebar de filtros.
3. **Detalhe (/produtos/:id):** Galeria de imagens, specs em tabela, botão adicionar ao carrinho, reviews
4. **Login/Cadastro (/login, /cadastro):** Reactive Forms com validações
5. **Carrinho (/carrinho):** Lista de itens, qty, total, botão finalizar (mock por enquanto)
6. **Meus Pets (/meus-pets):** CRUD dos pets do usuário (cards com fotos placeholder)
7. **Perfil (/perfil):** Dados do usuário, alterar senha

Header global com: logo, busca, ícone carrinho (badge com qtd), menu usuário
Footer com links institucionais
</paginas_e_funcionalidades>

<padroes>
- Componentes standalone, OnPush change detection sempre
- Signals para estado local; service com Signal para estado compartilhado (ex: CartService)
- Lazy loading por feature
- Interceptor adiciona Bearer token automaticamente
- Interceptor trata 401 (redireciona para login) e 500 (toast de erro)
- Loading state visual em todas as chamadas HTTP
- Tratamento de erro amigável
</padroes>

<design>
Paleta pet tech moderna:
- Primary: #6366F1 (índigo) ou #10B981 (verde tech)
- Acentos: tons de laranja/amarelo para CTAs
- Tipografia: Inter ou Poppins (Google Fonts)
- Cards com sombras suaves, bordas arredondadas (12px)
- Animações sutis nas interações
- Responsivo: mobile-first, breakpoints sm/md/lg/xl
- Dark mode bônus (toggle no header)
</design>

<entregaveis>
1. Projeto Angular completo (ng new + ajustes)
2. Todas as features funcionando integradas com a API
3. environment.ts e environment.prod.ts
4. README.md com: instalação, scripts, como configurar API URL, screenshots
5. Estrutura SCSS com design tokens em _variables.scss e _mixins.scss
6. ESLint + Prettier configurados
</entregaveis>

<processo>
1. Comece pelo scaffold + estrutura de pastas
2. Configure interceptors e services de autenticação
3. Implemente o fluxo de login (sem isso, nada funciona)
4. Depois construa páginas de leitura (home, catálogo, detalhe)
5. Por último, áreas autenticadas (pets, perfil, carrinho)
6. Faça auto-revisão de acessibilidade (alt em imagens, labels em forms, contraste)
</processo>
```

---

## FASE 3 — Containerização com Docker

### 🎯 Objetivo
Empacotar tudo em containers prontos para produção.

### ✅ Entregáveis
- Dockerfile multi-stage backend (imagem < 200MB)
- Dockerfile frontend com Nginx
- docker-compose.yml com toda stack local
- Health checks e graceful shutdown

### 📝 Prompt para Claude Code

```xml
<role>
Você é um DevOps Engineer especialista em containerização Java/Node, com foco em imagens pequenas, seguras e rápidas para build em CI/CD.
</role>

<contexto>
Projeto Pet-Hub Store já possui:
- Backend Spring Boot 3 em /backend (Maven, Java 21)
- Frontend Angular 17 em /frontend (npm)

Preciso containerizar tudo de forma production-ready e rodar localmente via docker-compose.
</contexto>

<entregaveis>

### 1. backend/Dockerfile (multi-stage)
- Stage 1 (build): maven:3.9-eclipse-temurin-21 → mvn clean package -DskipTests
- Stage 2 (runtime): eclipse-temurin:21-jre-alpine (ou distroless gcr.io/distroless/java21)
- Usuário não-root
- ENTRYPOINT com java -jar
- HEALTHCHECK via /actuator/health
- Expor 8080
- JVM tuning: -XX:+UseG1GC -XX:MaxRAMPercentage=75

### 2. frontend/Dockerfile (multi-stage)
- Stage 1: node:20-alpine → npm ci → ng build --configuration production
- Stage 2: nginx:1.27-alpine → copia dist/ → configura nginx.conf
- nginx.conf com:
  - try_files para SPA routing
  - gzip ligado
  - cache de assets
  - headers de segurança (X-Frame-Options, CSP básico)
- Expor 80

### 3. docker-compose.yml na raiz
Serviços:
- postgres (postgres:16-alpine) com volume nomeado, healthcheck
- backend (build do backend/, depends_on postgres healthy, env DATABASE_URL etc)
- frontend (build do frontend/, depends_on backend, porta 4200:80)
- pgadmin (bônus para inspecionar DB)

Configurações:
- Network bridge "pethub-net"
- Volumes: postgres-data, pgadmin-data
- Variáveis de ambiente via arquivo .env (forneça .env.example)
- Restart policy: unless-stopped

### 4. .dockerignore para backend e frontend
Exclua: node_modules, target, .git, .idea, *.md, .env, dist/, etc.

### 5. Makefile na raiz com targets:
- make up         → docker-compose up -d --build
- make down       → docker-compose down
- make logs       → docker-compose logs -f
- make clean      → down + remove volumes
- make rebuild    → down + up --build
- make ps         → docker-compose ps

### 6. README adicional (DOCKER.md)
- Pré-requisitos (Docker 24+, Compose v2)
- Como subir: cp .env.example .env && make up
- URLs: frontend localhost:4200, backend localhost:8080/swagger-ui.html
- Como inspecionar logs
- Troubleshooting comum
</entregaveis>

<requisitos>
- Imagem final do backend < 250MB
- Imagem final do frontend < 80MB
- Build do backend reaproveitando layer de dependências Maven
- Build do frontend reaproveitando layer de node_modules
- Nenhum secret commitado (apenas .env.example)
- Backend espera Postgres estar healthy antes de iniciar (wait-for-it ou healthcheck do compose)
</requisitos>

<processo>
1. Crie os .dockerignore primeiro
2. Backend Dockerfile + teste build local mentalmente
3. Frontend Dockerfile + nginx.conf
4. docker-compose orquestrando tudo
5. Makefile e documentação
6. Auto-revisão: imagens são pequenas? não rodam como root? healthchecks funcionam?
</processo>
```

---

## FASE 4 — Mensageria com Kafka (Microserviços)

### 🎯 Objetivo
Quebrar o monolito em microserviços orquestrados por eventos via Kafka.

### ✅ Entregáveis
- Separar order-service do catalog-service
- Kafka como event bus
- Saga pattern para fluxo de pedido
- Schema Registry (Avro) bônus

### 📝 Prompt para Claude Code

```xml
<role>
Você é um arquiteto de microserviços event-driven, especialista em Apache Kafka, Spring Cloud Stream, e padrões como Saga, Outbox, e CQRS.
</role>

<contexto>
O monolito da Fase 1 do Pet-Hub Store precisa evoluir. Vou extrair o módulo de pedidos para um microserviço próprio (order-service), comunicando-se com catalog-service via Kafka.
</contexto>

<arquitetura_alvo>
Serviços:
1. **catalog-service** (ex-monolito, mantém produtos, categorias, usuários, pets, reviews)
2. **order-service** (NOVO): gerencia carrinho, pedidos, pagamentos (mockados)
3. **Kafka** (confluent/cp-kafka:7.5 + zookeeper ou KRaft)
4. **Schema Registry** (confluent/cp-schema-registry)
5. **Kafka UI** (provectus/kafka-ui) para inspecionar tópicos

Fluxo do pedido (Saga coreografada):
1. Usuário cria pedido → order-service publica `OrderCreated`
2. catalog-service consome → reserva estoque → publica `StockReserved` ou `StockFailed`
3. order-service consome → se OK, publica `PaymentRequested` (mock aprova) → `OrderConfirmed`
4. catalog-service consome `OrderConfirmed` → debita estoque definitivamente
5. Em qualquer falha: compensação via `OrderCancelled` + `StockReleased`
</arquitetura_alvo>

<topicos_kafka>
- order-events (particionado por orderId)
- stock-events
- payment-events
- order.DLT (dead letter)

Retention: 7 dias
Partições: 3
Replicas: 1 (local), 3 (produção)
</topicos_kafka>

<stack>
- Spring Boot 3 + Spring Kafka (ou Spring Cloud Stream com binder Kafka)
- Schema Registry + Avro (apache.avro + confluent serializer)
- Idempotência: producer com acks=all, enable.idempotence=true
- Consumer groups separados por serviço
- Padrão Outbox para garantia transacional (tabela outbox + Debezium OU job de polling simples)
</stack>

<entregaveis>

### 1. Criar order-service do zero
Estrutura espelha catalog-service (Clean Arch).
Entidades: Order, OrderItem, OrderStatus (PENDING/CONFIRMED/CANCELLED/FAILED)
Endpoints REST:
- POST /api/orders (cria carrinho/pedido)
- GET /api/orders/{id}
- GET /api/orders (do usuário autenticado)

### 2. Modificar catalog-service
- Adicionar listener Kafka para eventos de pedido
- Implementar reserva e baixa de estoque transacional
- Publicar eventos de estoque

### 3. Schemas Avro
- OrderCreatedEvent
- StockReservedEvent / StockFailedEvent
- PaymentRequestedEvent / PaymentApprovedEvent / PaymentFailedEvent
- OrderConfirmedEvent / OrderCancelledEvent

Versione os schemas (compatibilidade BACKWARD).

### 4. Padrão Outbox
- Tabela `outbox_events` em ambos serviços
- Job @Scheduled a cada 1s lê eventos não publicados, envia ao Kafka, marca como publicado
- Transação garante: salvar agregado + outbox event no mesmo commit

### 5. Testes
- Testcontainers com Kafka + Postgres
- Testes end-to-end do fluxo da saga
- Teste de cenário de falha (estoque insuficiente → compensação)

### 6. docker-compose.yml atualizado
Adicionar: zookeeper, kafka, schema-registry, kafka-ui, order-service

### 7. Documentação
- ARCHITECTURE.md com diagrama de sequência do fluxo de pedido
- Como acessar Kafka UI (localhost:8090)
- Como inspecionar tópicos via linha de comando

</entregaveis>

<padroes_de_codigo>
- Eventos imutáveis (records Java)
- Listeners idempotentes (verificar se evento já foi processado via tabela `processed_events`)
- Logs estruturados com correlationId em todos os eventos
- @KafkaListener com containerFactory configurada para retry exponencial
- DLT (Dead Letter Topic) para eventos que falharem N vezes
</padroes_de_codigo>

<processo>
1. Defina os schemas Avro primeiro (contrato entre serviços)
2. Configure Kafka no docker-compose e valide via Kafka UI
3. Implemente outbox no catalog-service
4. Crie order-service esqueleto com endpoints REST
5. Implemente publish/consume nos dois lados
6. Teste happy path
7. Teste cenários de falha e compensação
8. Auto-revisão: idempotência garantida? DLT configurada? logs com trace?
</processo>
```

---

## FASE 5 — RabbitMQ para Notificações

### 🎯 Objetivo
Adicionar notification-service usando RabbitMQ, mostrando domínio de ambos brokers e quando usar cada um.

### 📝 Prompt para Claude Code

```xml
<role>
Você é especialista em mensageria assíncrona, com domínio de Kafka E RabbitMQ. Sabe quando usar cada um e implementa padrões corretos: exchanges, routing keys, DLQ, retry com backoff.
</role>

<contexto>
Pet-Hub Store já usa Kafka para eventos de domínio (Fase 4). Agora preciso adicionar um notification-service que envia notificações ao usuário (email, push, SMS mock) reagindo a eventos.

Por que RabbitMQ aqui (e não Kafka)?
- Notificações são fire-and-forget, com routing complexo por tipo/canal
- Volume relativamente baixo, mas exige roteamento por preferência do usuário
- Retry com backoff e DLQ são mais simples de configurar no RabbitMQ
- Demonstra que sei escolher a ferramenta certa para cada caso (importante mencionar no README)
</contexto>

<arquitetura>
- Order-service publica eventos no Kafka (já existe)
- Um adapter/bridge consome do Kafka e republica no RabbitMQ com routing key apropriada
  (alternativa: order-service publica nos dois — discuta trade-offs no README)
- notification-service consome do RabbitMQ e despacha para canais

Exchanges RabbitMQ:
- `notifications.topic` (topic exchange)
  - Routing keys: `notification.email.order.confirmed`, `notification.push.order.shipped`, etc.
- `notifications.dlx` (dead letter exchange)

Filas:
- `email.queue` (bind: notification.email.#)
- `push.queue` (bind: notification.push.#)
- `sms.queue` (bind: notification.sms.#)
- `notifications.dlq` (consome do DLX)
</arquitetura>

<stack>
- Spring Boot 3 + Spring AMQP
- RabbitMQ 3.13 + management plugin
- Template Jinja-like via Thymeleaf para conteúdo do email
- Mock providers (logam no console em vez de enviar de verdade)
</stack>

<entregaveis>

### 1. notification-service novo
Estrutura Clean Arch.
- Consumidores por canal (EmailConsumer, PushConsumer, SmsConsumer)
- Providers mockados: console + arquivo de log
- Configuração de retry: 3 tentativas com backoff exponencial (1s, 4s, 16s)
- Após esgotar retries → DLQ

### 2. Bridge Kafka → RabbitMQ
Pode ser dentro do order-service ou um pequeno gateway separado.
Lê eventos relevantes (OrderConfirmed, OrderShipped, PaymentApproved) e publica no RabbitMQ.

### 3. Template de email
Templates HTML simples em resources/templates/email/
- order-confirmed.html
- payment-approved.html

### 4. Preferências de notificação (bônus)
- Endpoint no catalog-service: GET/PUT /api/users/me/notification-preferences
- Antes de enviar, notification-service consulta preferências (cache em Redis 5min)

### 5. docker-compose atualizado
- Adicionar: rabbitmq:3.13-management (UI em :15672)
- Adicionar: redis:7-alpine (para cache de preferências)
- Adicionar: notification-service

### 6. Documentação
- MESSAGING.md comparando Kafka vs RabbitMQ neste projeto (quando uso cada um e por quê)
- Tabela de exchanges/filas/routing keys
- Como acessar RabbitMQ Management (guest/guest)

</entregaveis>

<padroes>
- Consumers idempotentes
- Mensagens com TTL configurável
- Acknowledge manual (ack após processamento bem-sucedido)
- Tratamento explícito de exceções no consumer
- Métricas: contadores de mensagens enviadas/falhas por canal
</padroes>

<processo>
1. Suba RabbitMQ no compose e crie a topologia (exchanges, filas, bindings) via @Configuration
2. Implemente o notification-service consumindo de uma fila simples
3. Adicione bridge Kafka→RabbitMQ
4. Configure retry e DLQ
5. Adicione preferências do usuário com cache
6. Teste fim-a-fim: criar pedido → confirmar → ver email mockado no log
</processo>
```

---

## FASE 6 — IA Generativa: Recomendações Inteligentes

### 🎯 Objetivo
Integrar Claude API (ou OpenAI) para recomendações personalizadas e chatbot pet.

### 📝 Prompt para Claude Code

```xml
<role>
Você é um engenheiro de IA aplicada, especialista em integrar LLMs em produtos via API, com domínio de prompt engineering, RAG (Retrieval Augmented Generation), e padrões de uso responsável de IA.
</role>

<contexto>
Pet-Hub Store precisa de um novo microserviço (recommendation-service) que usa IA generativa para:
1. Recomendar produtos baseado no perfil do pet do usuário (raça, idade, comportamento, histórico de compras)
2. Chatbot "Assistente Pet" que tira dúvidas sobre cuidados + sugere produtos
3. Gerar descrições ricas de produtos a partir de specs (ferramenta admin)
4. Resumir e analisar sentimento de reviews
</contexto>

<stack>
- Spring Boot 3 + Spring AI (https://docs.spring.io/spring-ai)
- Provider configurável: Anthropic Claude (preferido) OU OpenAI
- PGVector (extensão do PostgreSQL) para embeddings → RAG sobre catálogo
- Cache Redis para respostas frequentes (TTL 1h)
- Rate limiting por usuário (Bucket4j)
</stack>

<funcionalidades>

### 1. POST /api/ai/recommendations
Input: petId
Processo:
- Busca dados do pet + histórico de pedidos do dono
- Busca produtos relevantes via similaridade vetorial (RAG)
- Monta prompt estruturado com XML tags
- Chama LLM
- Retorna lista de produtos recomendados + justificativa em linguagem natural

Prompt template (use XML tags como você já sabe):
```
<contexto>Você é o assistente do Pet-Hub Store...</contexto>
<pet>{dados_do_pet}</pet>
<historico>{ultimos_pedidos}</historico>
<produtos_disponiveis>{produtos_via_rag}</produtos_disponiveis>
<tarefa>Recomende 3-5 produtos com justificativa personalizada</tarefa>
<formato_resposta>JSON estrito: {"recomendacoes":[{"produtoId":1,"razao":"..."}]}</formato_resposta>
```

### 2. POST /api/ai/chat
Conversa multi-turno. Mantém histórico em Redis por sessionId.
System prompt: define escopo (só fala de pets e produtos da loja), tom amigável, em PT-BR.
Streaming SSE para resposta progressiva.

### 3. POST /api/ai/products/{id}/generate-description (ADMIN)
Recebe specs do produto, gera descrição comercial atraente.

### 4. POST /api/ai/reviews/analyze (job batch ou síncrono)
Para cada review nova: nota de sentimento (-1 a 1), tags extraídas, possíveis problemas reportados.

### 5. RAG do catálogo
- Job que gera embeddings de todos os produtos (nome + descrição + specs)
- Armazena em tabela `product_embeddings` (PGVector)
- Consulta por similaridade do cosseno
- Re-indexação automática quando produto é atualizado (via Kafka event)
</funcionalidades>

<requisitos_nao_funcionais>
- Provider abstraído via interface `LLMProvider` → implementações `AnthropicProvider`, `OpenAIProvider`
- Configuração via application.yml: qual provider, modelo, temperatura, maxTokens
- Logs de prompts e respostas (sem dados sensíveis) para auditoria
- Custo estimado por requisição logado (tokens × preço)
- Timeout: 30s, com fallback gracioso
- Rate limit: 10 req/min por usuário em endpoints de IA
- Não vaze chave de API em logs nem responses de erro
</requisitos_nao_funcionais>

<entregaveis>
1. Microserviço recommendation-service completo
2. Schema PGVector e job de embeddings
3. Frontend Angular: componente de chat (botão flutuante), seção "Recomendado para [nome do pet]" na home
4. docker-compose atualizado (já tem Postgres; só precisa habilitar extensão pgvector)
5. AI.md documentando: provider usado, custos estimados, exemplos de prompts, considerações éticas (alucinação, viés)
6. .env.example com placeholder para ANTHROPIC_API_KEY
</entregaveis>

<processo>
1. Configure Spring AI com provider escolhido
2. Implemente endpoint mais simples (generate-description) para validar integração
3. Configure PGVector + job de embeddings
4. Implemente recomendações com RAG
5. Implemente chat com histórico
6. Conecte ao frontend (componente flutuante)
7. Auto-revisão: prompts estão protegidos contra prompt injection? rate limit funciona? custos estão visíveis?
</processo>
```

---

## FASE 7 — Kubernetes Local

### 🎯 Objetivo
Manifests K8s para toda a stack, rodando em Minikube/Kind.

### 📝 Prompt para Claude Code

```xml
<role>
Você é um engenheiro Kubernetes Certified (CKA), especialista em deploy de aplicações Spring Boot e Angular em clusters K8s, com práticas de produção (probes, recursos, secrets, autoscaling).
</role>

<contexto>
Pet-Hub Store tem 4 microserviços + Postgres + Kafka + RabbitMQ + Redis. Preciso de manifests K8s production-grade para rodar primeiro em Minikube/Kind e depois deployar em cloud (Fase 8).
</contexto>

<entregaveis>

### 1. Estrutura infrastructure/k8s/
```
k8s/
├── namespace.yaml
├── configmaps/
├── secrets/                 (com placeholders, NÃO valores reais)
├── databases/
│   ├── postgres-statefulset.yaml
│   ├── postgres-service.yaml
│   └── postgres-pvc.yaml
├── messaging/
│   ├── kafka/
│   └── rabbitmq/
├── cache/
│   └── redis.yaml
├── services/
│   ├── catalog-service/
│   ├── order-service/
│   ├── notification-service/
│   ├── recommendation-service/
│   └── frontend/
├── ingress/
│   └── ingress.yaml
└── kustomization.yaml       (raiz, com overlays dev/prod)
```

### 2. Para cada microserviço, gere:
- Deployment com:
  - 2 réplicas (3 em prod)
  - resources.requests e resources.limits
  - livenessProbe e readinessProbe via /actuator/health
  - startupProbe (Spring Boot demora a subir)
  - env vars via ConfigMap + Secret
  - securityContext: runAsNonRoot, readOnlyRootFilesystem onde possível
  - imagePullPolicy: IfNotPresent
- Service ClusterIP
- HPA (HorizontalPodAutoscaler) baseado em CPU 70% e memória 80%, min 2, max 10

### 3. Postgres como StatefulSet
- PVC de 5Gi
- Service headless
- initContainer para criar extensão pgvector

### 4. Kafka
Use Strimzi Operator OU bitnami/kafka (KRaft mode, sem zookeeper) — escolha um e justifique.
3 brokers para simular HA.

### 5. RabbitMQ
StatefulSet com 1 réplica (cluster de 3 é bônus).
Habilitar plugins: management, prometheus.

### 6. Ingress
- Nginx Ingress Controller
- Host: pethub.local (instruir a editar /etc/hosts)
- Paths:
  - / → frontend
  - /api/catalog/* → catalog-service
  - /api/orders/* → order-service
  - /api/ai/* → recommendation-service
- TLS com cert-manager + Let's Encrypt staging (instruções, opcional local)

### 7. Kustomize com overlays
- base/ (manifests acima)
- overlays/dev/ (replicas reduzidas, recursos menores)
- overlays/prod/ (replicas full, recursos production)

### 8. Helm chart equivalente (bônus)
Em infrastructure/helm/pethub/, transformar tudo em chart parametrizável.

### 9. Scripts e documentação
- scripts/k8s-setup.sh: instala minikube/kind + addons + aplica tudo
- scripts/k8s-teardown.sh
- K8S.md com:
  - Pré-requisitos (kubectl, kustomize, minikube/kind)
  - Como subir: ./scripts/k8s-setup.sh
  - Como verificar: kubectl get pods -n pethub
  - Como acessar: editar /etc/hosts e abrir http://pethub.local
  - Troubleshooting (CrashLoopBackOff, ImagePullBackOff)

</entregaveis>

<requisitos>
- Nenhum container roda como root
- Todos os secrets como Secret (mesmo que placeholder)
- Labels consistentes: app, component, environment, version
- Annotations para Prometheus scrape (prometheus.io/scrape: "true", port, path)
- Anti-affinity para distribuir réplicas entre nós (em prod)
- PodDisruptionBudget para serviços críticos
</requisitos>

<processo>
1. Comece pelo namespace e configmaps/secrets
2. Suba os "stateful" primeiro: Postgres, Kafka, RabbitMQ, Redis
3. Depois os microserviços
4. Por último o ingress
5. Valide tudo mentalmente: dependências entre serviços, probes apropriadas, recursos realistas
6. Crie o script de setup automatizado
</processo>
```

---

## FASE 8 — Deploy em Cloud Pública (AWS ou Oracle)

### 🎯 Objetivo
Deployar em cloud real com IaC, CI/CD e serviços gerenciados.

### 📝 Prompt para Claude Code

```xml
<role>
Você é um Cloud Engineer Senior com certificações em AWS e Oracle Cloud, especialista em Terraform, EKS/OKE, e CI/CD com GitHub Actions.
</role>

<contexto>
Pet-Hub Store está pronto em K8s local (Fase 7). Hora de deployar em cloud pública.
Cloud escolhida: [ESCOLHA: AWS com EKS, ou Oracle Cloud com OKE — Oracle tem free tier mais generoso, mas AWS é mais comum no mercado brasileiro].
</contexto>

<arquitetura_cloud>
[Se AWS:]
- VPC com 3 AZs, subnets pública/privada
- EKS cluster (2 nós t3.medium, autoscaling até 5)
- RDS PostgreSQL (db.t3.micro, multi-AZ false para economia)
- MSK Serverless (Kafka gerenciado) OU Amazon MQ para RabbitMQ
- ElastiCache Redis (cache.t3.micro)
- ECR para imagens Docker
- ALB via AWS Load Balancer Controller
- CloudWatch para logs/métricas
- Secrets Manager
- Route53 + ACM para domínio + HTTPS (se tiver domínio)

[Se Oracle Cloud:]
- VCN com subnets pública/privada
- OKE cluster
- Autonomous Database (Always Free) ou Base Database
- OCI Streaming (Kafka-compatible) ou self-managed
- OCI Registry para imagens
- Load Balancer
- OCI Logging + Monitoring
- OCI Vault para secrets
</arquitetura_cloud>

<entregaveis>

### 1. Terraform em infrastructure/terraform/
Módulos:
- vpc/
- eks/ (ou oke/)
- rds/
- msk/ (ou similar)
- ecr/ (ou ocir/)
- iam/

Backend remoto: S3 + DynamoDB (AWS) ou OCI Object Storage.
Variáveis por ambiente (dev/staging/prod) via .tfvars.

### 2. CI/CD com GitHub Actions
Workflows em .github/workflows/:
- ci.yml: roda em PR — lint, testes unitários e integração, build, scan de vulnerabilidades (Trivy)
- cd-dev.yml: push em main → build imagens → push ECR → deploy EKS dev
- cd-prod.yml: tag v* → build → ECR → deploy EKS prod (com approval manual)

Etapas do CD:
1. Configure AWS credentials via OIDC (sem secrets longos)
2. Login ECR
3. docker build + push com tag = git sha + 'latest'
4. kubectl apply usando kustomize overlay correspondente
5. kubectl rollout status (espera deploy completar)
6. Slack/Discord webhook de notificação

### 3. Manifests K8s ajustados para cloud
- imagePullSecrets para ECR
- StorageClass apropriada (gp3 na AWS)
- Anotações específicas para ALB Ingress Controller
- ExternalSecrets Operator integrando com Secrets Manager (bônus)

### 4. Observabilidade básica
- CloudWatch Container Insights habilitado
- Logs estruturados (JSON) dos serviços indo para CloudWatch
- Alarms básicos: CPU > 80% por 5min, pod restart > 3 em 10min

### 5. Documentação CLOUD.md
- Custos estimados (deixe claro o que cabe em free tier e o que não)
- Como aplicar Terraform passo a passo
- Como configurar GitHub Actions (secrets necessários, OIDC trust)
- Como destruir tudo (terraform destroy) — IMPORTANTE para evitar conta surpresa
- Diagrama de arquitetura cloud (texto/mermaid)

</entregaveis>

<requisitos>
- Tudo "destrutível" em um comando (custos sob controle)
- Nenhum secret no Terraform (use Secrets Manager / Vault)
- IAM com princípio do menor privilégio
- Tags em todos os recursos: project=pethub, env, owner
- HTTPS obrigatório no Ingress
- Network policies básicas no K8s
</requisitos>

<processo>
1. Decida a cloud e justifique no README (custo, familiaridade, free tier)
2. Crie módulos Terraform incrementalmente: VPC → cluster → bancos → messaging
3. Valide com `terraform plan` antes de aplicar
4. Após cluster pronto, ajuste manifests K8s e aplique
5. Configure GitHub Actions
6. Faça um deploy completo end-to-end
7. Documente custos reais incorridos
8. Auto-revisão: tudo tem destroy? secrets seguros? observabilidade mínima?
</processo>
```

---

## FASE 9 — Observabilidade e Polimento Final

### 🎯 Objetivo
Adicionar Prometheus, Grafana, tracing distribuído, e preparar o projeto para mostrar em portfólio/entrevistas.

### 📝 Prompt para Claude Code

```xml
<role>
Você é SRE Senior, especialista em observabilidade (três pilares: métricas, logs, traces), e em criar projetos open-source que vendem o talento de quem os escreveu.
</role>

<contexto>
Última fase do Pet-Hub Store. Tudo funciona em cloud. Falta:
1. Stack de observabilidade
2. README absolutamente impecável
3. Vídeo demo curto (script)
4. Arquitetura documentada visualmente
</contexto>

<entregaveis>

### 1. Stack de observabilidade
Adicione ao K8s e ao docker-compose:
- Prometheus (scrape de /actuator/prometheus dos Spring Boot)
- Grafana com dashboards pré-configurados:
  - JVM metrics (heap, GC, threads)
  - HTTP requests (rate, errors, latency p95/p99)
  - Kafka consumer lag
  - RabbitMQ queue depth
  - Postgres connections
  - Custom business metrics (pedidos/min, recomendações geradas, etc)
- Loki para logs centralizados (ou ELK)
- Tempo ou Jaeger para distributed tracing
- OpenTelemetry agent nos serviços Spring Boot

### 2. Instrumentação custom
- Métricas de negócio com Micrometer (@Counted, @Timed)
- Trace correlation IDs propagados via Kafka headers e RabbitMQ headers
- Logs estruturados (JSON) com MDC

### 3. README.md raiz repaginado
Estrutura sugerida:
- Header com badges (build status, coverage, license)
- GIF/screenshot do produto rodando
- Why this project (problema que resolve)
- Tech stack visual (ícones)
- Arquitetura (diagrama Mermaid ou link para imagem)
- Features
- Como rodar (3 opções: docker-compose, k8s local, cloud)
- Estrutura do monorepo
- Roadmap
- Sobre o autor (você, Ali)

### 4. ARCHITECTURE.md
- Modelo C4: contexto, containers, componentes
- Decisões arquiteturais (ADRs) das principais escolhas:
  - ADR-001: Por que microserviços (e quando não usar)
  - ADR-002: Kafka para eventos de domínio
  - ADR-003: RabbitMQ para notificações
  - ADR-004: PGVector para RAG (vs Pinecone/Weaviate)
  - ADR-005: Spring AI vs LangChain4j

### 5. Script de demo (DEMO.md)
Roteiro de 5 minutos para gravar vídeo mostrando:
- Login no app
- Cadastrar pet
- Ver recomendações geradas pela IA
- Conversar com o chatbot
- Fazer um pedido
- Mostrar evento percorrendo Kafka → estoque → RabbitMQ → notificação
- Dashboard Grafana com a request aparecendo
- Logs centralizados com trace ID
- kubectl get pods mostrando tudo rodando

### 6. CONTRIBUTING.md, LICENSE (MIT ou Apache 2.0), CHANGELOG.md

### 7. Limpeza geral
- Remover TODOs/FIXMEs pendentes
- Garantir que README de cada subprojeto está consistente
- Verificar que todos os links funcionam
- Padronizar nomes (kebab-case em pastas, PascalCase em classes)
- Rodar formatador em todo o código

</entregaveis>

<processo>
1. Stack de observabilidade primeiro (mais técnico)
2. Instrumente os serviços e valide nos dashboards
3. Refaça o README raiz como se fosse um landing page do produto
4. Documente arquitetura com diagramas
5. Grave vídeo (ou apenas escreva roteiro polido)
6. Limpeza e revisão final
</processo>
```

---

# 📅 Cronograma Sugerido

| Fase | Esforço estimado (horas focadas) | Pode pular? |
|------|----------------------------------|-------------|
| 1 — Backend Core | 15-20h | Não |
| 2 — Frontend Angular | 12-15h | Não |
| 3 — Docker | 4-6h | Não |
| 4 — Kafka | 15-20h | Não |
| 5 — RabbitMQ | 8-10h | Não (diferencial) |
| 6 — IA Generativa | 10-15h | Não (grande diferencial atual) |
| 7 — Kubernetes | 10-12h | Não |
| 8 — Cloud | 8-12h | Pode adiar (custo) |
| 9 — Observabilidade | 8-10h | Pode simplificar |

**Total:** ~90-120h de trabalho focado.

---

# 💡 Dicas Estratégicas (V1)

1. **Faça commits pequenos e frequentes** no GitHub público — recrutadores olham o histórico.
2. **Cada fase = uma branch + PR + merge**. Mostra que você sabe trabalhar em equipe.
3. **Use Conventional Commits** (feat:, fix:, docs:, refactor:).
4. **Documente decisões erradas também** nos ADRs — mostra senioridade.
5. **Não tente fazer tudo perfeito de primeira**. Termine a Fase 1 funcionando antes de tocar na 2.
6. **Para entrevistas:** mesmo que esteja na Fase 4, já vale mostrar. Mostre o repo e diga "estou na fase X do roadmap, próximo é Y".
7. **LinkedIn:** ao terminar cada fase, faça um post curto contando o que aprendeu.

---

# 🎯 Próximos Passos Imediatos (V1)

1. Criar repositório no GitHub: `pet-hub-store`
2. Copiar este plano como `ROADMAP.md` no repo
3. Abrir Claude Code no diretório vazio
4. Colar o **prompt da Fase 1** e deixar o agente trabalhar
5. Revisar, testar, ajustar, commitar
6. Repetir para próximas fases
