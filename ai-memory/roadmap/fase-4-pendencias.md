# Fase 4 — Pendências e checklist

> **Status:** Fase 4 ✅ entregue em 2026-05-14. 2 commits validados E2E.
> Testes formais e JaCoCo continuam como dívida técnica (mesmo bloqueio das Fases 1/2/3).

## ✅ Commits entregues nesta fase (2)

1. `1cb496f feat(inventory,order): add stock reservations and order state machine`
   - **Módulo `pet-hub-inventory`** com entidades `Estoque` (`@OneToOne` para Produto, com `quantidade`, `quantidade_reservada`, `quantidade_minima`, `localizacao`), `MovimentacaoEstoque` (audit trail dos 6 tipos), `ReservaEstoque` (TTL 15min com 4 status). V14 cria schema com check constraints (`reservada <= qty`, `qty >= 0`) e partial index para low-stock.
   - `InventoryService.reservar()` usa lock pessimista (`@Lock PESSIMISTIC_WRITE`) para evitar race condition (10 threads brigando pelo último item → só 1 sucede). Ordena itens por SKU antes do loop para evitar deadlock cruzado.
   - `confirmarReservas()` decrementa `quantidade` E `quantidade_reservada` simultaneamente + cria movimentação `BAIXA_VENDA`. `liberarReservas()` decrementa só `reservada` + cria `LIBERACAO_RESERVA` (`CANCELADA` ou `EXPIRADA`).
   - `@Scheduled ReservaExpiracaoJob` roda a cada 60s, libera reservas vencidas, marca status `EXPIRADA`. **OBS:** cancelamento do pedido em si fica para o order-service (separação de bounded contexts).
   - **Módulo `pet-hub-order`** com entidades `Pedido` (snapshots JSONB de endereço/frete — desacoplado de mudanças vivas), `PedidoItem` (snapshot do produto), `PedidoEvento` (audit tipado). V15 cria tabelas + `pedido_sequence(ano, ultimo_numero)`.
   - `OrderNumberGenerator.next()` retorna `PH-YYYY-NNNNNN` com lock pessimista no incremento. Anota `REQUIRES_NEW` para liberar o lock cedo.
   - `OrderStateMachine`: 8 estados, 3 terminais (PAGAMENTO_REJEITADO, CANCELADO, DEVOLVIDO). Tentativa inválida → `BusinessRuleException` (422).
   - `OrderService.transicionar()` aciona hooks: APROVADO confirma reservas, REJEITADO libera, CANCELADO pré-pagamento libera. **Cancelamento pós-aprovado dispara WARN** (estorno manual de estoque + refund — não silenciamos o financeiro).
   - `TimelineService` monta 5 etapas felizes (Pedido recebido, Pagamento aprovado, Em separação, Em transporte, Entregue) com tempo decorrido entre cada uma (instantâneo / Xmin / Xh / Xdias). Estados terminais não-felizes têm timeline própria (Pedido recebido + estado terminal).
   - Endpoints expostos: `GET /api/v1/orders[/{numero}|/timeline|/cancel]` (cliente), `GET /api/v1/admin/orders[/stats|/{numero}|/transition]` (admin com `@PreAuthorize`), `GET /api/v1/admin/inventory[/low-stock|/{sku}|/{sku}/history|/movements]`.
   - `@EnableScheduling` adicionado em `PetHubApplication`.

2. `d6460a5 feat(checkout): wire place-order into inventory + order with deterministic mock`
   - `/checkout/preview` valida estoque cedo (422 listando SKUs insuficientes) para evitar UX ruim.
   - `/checkout/place-order` agora cria reservas → materializa pedido → cobra → transition (APROVADO/REJEITADO disparam hooks) → limpa carrinho. Idempotência mantida via `UNIQUE idempotency_key`.
   - `MockPaymentGateway` em modo determinístico (default a partir desta fase): cartão `last4=4000` rejeita; demais aprovam; PIX/BOLETO sempre aprovam. Modo probabilístico (~0.9) preservado via `payment.mock.mode=probabilistic`.
   - Token de cartão evolui para formato `tok_mock_<last4>_<uuid>` — last4 não é PII restrita (PCI-DSS 3.4), permite decisão de aprovação sem reler PAN.
   - Fix `cast(:q as string)` em `PedidoRepository.searchAdmin` para contornar o issue de bytea inference do Postgres (mesma classe de problema que tivemos em `UsuarioRepository.searchByTipo`).
   - Fix LazyInitializationException: controllers de transition/cancel recarregam via `detalheAdmin`/`detalheCliente` depois da mutação.

## ✅ Validações E2E acumuladas (2026-05-14)

- Build (`mvn clean install -DskipTests`) verde, 13 módulos no reactor.
- Maria adiciona 2x COLLAR-PRO-001 → preview `subtotal=1599.80, frete=39.81, total=1639.61`.
- Place-order com cartão approved (last4=1111) → `PH-2026-000001`, status APPROVED, `tentativaPagamentoId=2`.
- Estoque do SKU: 50 → 48 (decremento exato).
- `movimentacoes_estoque`: linha RESERVA(2) + linha BAIXA_VENDA(2), ambas com `referencia_pedido='PH-2026-000001'`.
- `reservas_estoque`: status CONFIRMADA, qty=2.
- 2ª chamada com mesma idem-key → mesma tentativa (`tentativaPagamentoId=2`), sem cobrar de novo.
- Cliente tenta cancelar pedido APROVADO → 422 (state machine recusa).
- Admin lista pedidos com `q=PH` retorna o pedido; stats por status correto.
- Admin transicionou APROVADO → SEPARACAO → EM_TRANSPORTE; timeline reflete `Em transporte = ATUAL` com tempo decorrido por etapa.
- Carrinho limpou após APPROVED.

## ⚠️ Dívida técnica restante (não bloqueia Fase 5)

### Cancelamento pós-pagamento aprovado
Hoje o hook só loga WARN — não estorna estoque automaticamente. Quando a Fase 7 (NF-e) ou Fase 8 (storefront avançado) precisar de "cancelar e estornar", criar:
- `InventoryService.reverterBaixa(referenciaPedido, motivo)` que para cada movimentação `BAIXA_VENDA` cria uma `ENTRADA` correspondente.
- `OrderService.cancelarPosAprovado()` que chama o reverte + `paymentGateway.refund()`.
- Hoje admin pode cancelar via `/admin/orders/{numero}/transition` mas a responsabilidade do estorno é manual (movimentação de inventory + refund via gateway).

### Webhook de pagamento assíncrono (PIX/BOLETO)
PIX e BOLETO hoje aprovam direto (mock determinístico devolve APPROVED). No mundo real PIX chega via webhook do gateway. Implementar quando entrar Mercado Pago / Stripe na Fase 7+:
- Endpoint `POST /webhooks/payment/{provider}` com validação HMAC.
- Pedido em PENDENTE_PAGAMENTO transiciona para APROVADO quando webhook chega com tx aprovada.

### Testes formais
Smoke E2E cobre happy path + idempotência + isolamento + state machine. Continuam pendentes (mesmo bloqueio das Fases 1/2/3 — Testcontainers em container Maven não roteia):
- `InventoryServiceTest`: reserva concorrente (10 threads disputando 1 unidade → 9 falham), expiração via clock injetável, reverte baixa.
- `OrderStateMachineTest`: tabela parametrizada com cada transição válida + cada inválida.
- `OrderNumberGeneratorTest`: 2 anos consecutivos, concorrência (100 threads → 100 números únicos).
- `TimelineServiceTest`: caminho feliz, terminais não-felizes, tempos decorridos formatados.
- `CheckoutServiceTest` ampliado: estoque insuficiente no preview → 422; cobrança REJEITADA libera reservas.

### Decisões a confirmar
- Pedido com cancelamento por cliente hoje só funciona em PENDENTE_PAGAMENTO. Talvez liberar para PAGAMENTO_APROVADO antes de SEPARACAO no storefront — exige estorno automático. Decidir na Fase 5.
- Threshold de quantidade_minima hoje seed = 5 para todos os SKUs. Quando admin entrar (Fase 6), expor edição.
- ReservaExpiracaoJob hoje é monolito-friendly. Para múltiplas réplicas (Fase 11), considerar lock distribuído (ShedLock + Redis) — anotado também em `appsec-pendencias.md` RL-1.

## 🚀 Comando rápido para retomar

```bash
# Verificar estado
cat ai-memory/roadmap/fase-4-pendencias.md
git log --oneline -5

# Subir infra + app via container Maven (host sem JDK/Maven nativo)
docker compose --env-file .env.local -f infrastructure/docker/docker-compose.dev.yml up -d
cd backend && docker run --rm -d --name pethub-app-dev \
  --env-file /home/ali/projects/pet-hub/.env.local \
  -v "$PWD":/workspace -v /home/ali/.m2:/root/.m2 \
  --network host -w /workspace \
  maven:3.9-eclipse-temurin-21 \
  mvn -B -ntp -pl application spring-boot:run -DskipTests

# Smoke check rápido
curl -s http://localhost:8080/actuator/health
# Maria (cliente) e admin disponíveis (seed):
# maria.fase2@pethub.com / Senha@123
# admin@pethub.com       / Admin@123
```

Próximo passo: abrir Fase 5 (Frontend Storefront em Angular) — sem dependência das pendências de teste.
