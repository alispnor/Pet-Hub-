# 🏛️ Pet Hub — Arquitetura

> Documento vivo. Esqueleto criado na Fase 0; expandido a cada fase conforme novos componentes entram em cena. Decisões com impacto estrutural ficam em [`docs/adr/`](./docs/adr/).

## Visão geral

Pet Hub é um e-commerce de produtos tecnológicos para pets construído como **monolito modular Java** que evolui para **microserviços event-driven** a partir da Fase 9 (ver [ADR-0002](./docs/adr/0002-arquitetura-evolutiva.md)).

A escolha de começar monolito modular evita "microservice fatigue" desnecessário enquanto o domínio ainda está sendo descoberto, e demonstra evolução arquitetural realista — o caminho que a maioria dos produtos reais percorre.

## Diagrama de contexto (C4 nível 1)

```
                    ┌────────────────────┐
                    │      Cliente       │
                    │   (browser/app)    │
                    └─────────┬──────────┘
                              │ HTTPS
                              ▼
┌─────────────────┐   ┌───────────────────┐   ┌──────────────────┐
│   Pet Diary     │   │                   │   │   Administrador   │
│   (ecossistema) │◄──┤     Pet Hub       │◄──┤   (back-office)  │
│   (sister app)  │   │                   │   │                  │
└─────────────────┘   └───┬───────────────┘   └──────────────────┘
                          │
       ┌──────────────────┼─────────────────────────────────────┐
       │                  │                 │                   │
       ▼                  ▼                 ▼                   ▼
┌───────────┐    ┌────────────────┐  ┌──────────────┐  ┌───────────────┐
│  Correios │    │ Gateway pagto  │  │  SEFAZ       │  │ Anthropic API │
│  / Frenet │    │ (MercadoPago,  │  │ (NF-e via    │  │ (Claude para  │
│  / Melhor │    │  Stripe)       │  │  Focus NFe)  │  │  recomendação │
│  Envio    │    │                │  │              │  │  + chatbot)   │
└───────────┘    └────────────────┘  └──────────────┘  └───────────────┘
```

Pet Hub é o **system in focus**. Tudo ao redor são atores ou sistemas externos.

## Módulos / serviços planejados

Estrutura definida em [`ai-memory/roadmap/plano-completo.md`](./ai-memory/roadmap/plano-completo.md). Cada item abaixo nasce como módulo Maven dentro de `backend/` (Fases 1–7) e pode virar deployable separado a partir da Fase 9:

| Módulo | Responsabilidade | Fase de origem |
|---|---|---|
| `common` | Exceptions base, GlobalExceptionHandler (RFC 7807), utils | 1 |
| `identity` | Auth (JWT), usuários, roles | 1 |
| `catalog` | Categorias, produtos, imagens, preços | 1 |
| `customer` | Perfil, pets, endereços, formas de pagamento | 2 |
| `cart` | Carrinho (Redis) | 3 |
| `pricing` | Cupons, promoções, regras de imposto | 3 |
| `shipping` | Cálculo de frete (estratégia + cache) | 3 |
| `payment` | Gateway abstrato + Mock + MP Sandbox | 3 |
| `checkout` | Orquestração do fluxo de compra | 3 |
| `order` | Pedidos, máquina de estados, timeline | 4 |
| `inventory` | Estoque, reservas, movimentações | 4 |
| `invoice` | Emissão de NF-e | 7 |
| `notification` | Email/push (RabbitMQ) | 9 |
| `recommendation` | IA generativa (Claude + RAG via PGVector) | 10 |
| `reporting` | Relatórios e analytics admin | 6+ |

### Frontend

| App | Responsabilidade | Fase |
|---|---|---|
| `frontend/storefront` | Loja virtual (Cliente B2C) | 5 |
| `frontend/admin` | Back-office (operadores/gerentes/admins) | 6 |

## Padrões adotados

### Clean Architecture (Hexagonal)

Cada módulo segue:

```
{modulo}/
├── domain/         entidades JPA, value objects, domain events
├── application/    use cases (services), DTOs (records), ports, mappers (MapStruct)
└── infrastructure/ adapters: REST controllers, JPA repositories, security
```

A regra de dependência é unidirecional: `infrastructure` → `application` → `domain`. Domínio não conhece infraestrutura.

### Event-Driven (a partir da Fase 9)

- **Kafka** para eventos de domínio (`OrderPaid`, `StockReserved`, `InvoiceIssued`, …) com Schema Registry + Avro.
- **RabbitMQ** para notificações e tasks com routing complexo (email, push, SMS).
- **Padrão Outbox** para garantia transacional: o evento é gravado na mesma transação do agregado e publicado por um job de polling.
- **Idempotência** em todos os consumers via tabela `processed_events`.

### CQRS leve (a partir da Fase 6)

Endpoints de leitura para o admin (relatórios, dashboards) usarão projeções/views otimizadas, separadas do modelo de escrita. Sem event sourcing completo — apenas separação de leitura e escrita onde faz sentido.

### Padrão de função

Todo método público de service segue o template (declarado pelo Ali como invariante do projeto):

```java
public Type metodo(Params params) {
    log.debug("Iniciando {} com params={}", "metodo", params);
    validar(params);
    var resultado = executarLogica(params);
    log.debug("Finalizado {} com resultado={}", "metodo", resultado);
    return resultado;
}
```

Ordem: log entrada → validação → lógica → log saída → retorno. Usar `@Slf4j` (Lombok).

### Decisões adicionais

- **DTOs como records Java** (imutáveis). Nunca classes com setters.
- **MapStruct** para todas as conversões entity ↔ DTO. Nunca conversão manual.
- **Exceptions de negócio** estendem `BusinessException` (em `common/`). `GlobalExceptionHandler` retorna RFC 7807.
- **Sem lógica de negócio em controllers** — controllers só delegam ao service.
- **Soft delete** (campo `ativo`) em vez de `DELETE` físico onde fizer sentido.
- **BigDecimal** para todo valor monetário, com `setScale(2, HALF_EVEN)` ao expor.
- **Conventional Commits** com scope por módulo (`feat(catalog):`, `feat(identity):`, etc.).

## Estratégia evolutiva

| Fase | Estado da arquitetura |
|---|---|
| 1–4 | Monolito modular Maven (`backend/application/` é o único deployable) |
| 5–7 | + Frontends Angular e invoice-service ainda dentro do monolito |
| 8 | Containerizado via Docker, mas ainda monolito |
| 9 | Extração de `order` e `notification` como deployables separados. Kafka entra em cena. |
| 10 | `recommendation-service` nasce já como microserviço |
| 11 | Tudo em Kubernetes (monolito ou microserviços) |
| 12 | Multi-serviço completo em cloud com IaC, CI/CD e observabilidade |

## Próximos passos

Esta arquitetura será expandida em cada fase com diagramas adicionais:

- **Fase 1:** diagrama de containers (C4 nível 2) com módulos e PostgreSQL
- **Fase 4:** diagrama de sequência do fluxo de pedido completo
- **Fase 9:** diagrama de eventos com tópicos Kafka e exchanges RabbitMQ
- **Fase 11:** topologia Kubernetes
- **Fase 12:** arquitetura cloud completa (AWS ou Oracle)

Diagramas em ASCII inline aqui; quando ficarem grandes, vão para `docs/diagrams/` em PlantUML/Mermaid.
