# Fase 3 — Pendências e checklist

> **Status:** Fase 3 ✅ entregue em 2026-05-14. 6 commits validados E2E.
> Pedido (entity completa + máquina de estados + estoque) entra na Fase 4.
> Testes formais e JaCoCo continuam como dívida técnica (mesmo bloqueio das Fases 1/2).

## ✅ Commits entregues nesta fase (6)

1. `8f86912 refactor(payment): extract gateway abstraction into its own module`
   - `pet-hub-payment` adicionado ao reactor
   - `PaymentGateway` (interface) + `MockPaymentGateway` movidos do `customer/` para `payment/`
   - `customer` passou a depender do módulo `payment` para o tokenize do cartão
   - Prepara terreno para `MercadoPagoGateway` em fases futuras sem mexer no customer

2. `dadfca2 feat(cart): add redis-backed shopping cart with 30-day ttl`
   - Módulo `pet-hub-cart`
   - `Cart`, `CartItem`, `CartCupom` como records em domínio
   - `CartRepository` em Redis com `RedisTemplate<String, Cart>` + Jackson, key `cart:{userId}`, TTL 30 dias
   - `CartService`: adicionar/atualizar/remover/limpar item, anexar/remover cupom (validação real fica para checkout)
   - Endpoints `/api/v1/cart` (GET/POST items, PUT/DELETE items/{sku}, POST/DELETE coupon, DELETE)
   - SKU validado contra `Produto` no catálogo; qty > 0

3. `39b30ae feat(pricing): add coupons, promotions, tax rules and price calculator`
   - Módulo `pet-hub-pricing`
   - Entidades: `Cupom` (PERCENTUAL/VALOR_FIXO, vigência, uso_max), `Promocao` (categoria/produto/marca), `RegraImposto` (NCM/UF)
   - V11: schema_pricing (cupons + promocoes + regras_imposto + seed `10OFF`, `FRETEGRATIS`, `EXPIRED`)
   - `PriceCalculatorService.calcular(items, cupom, uf, frete, userId)` retorna `PriceBreakdown` (subtotal, descontos, impostos, total)
   - Ordem: subtotal → promoções → cupom → impostos → +frete → total
   - 422 para cupom expirado/inativo, abaixo do mínimo, ou estourando uso por cliente

4. `762c522 feat(shipping): add three calculators with redis-cached shipping quotes`
   - Módulo `pet-hub-shipping`
   - `ShippingCalculator` strategy: `CorreiosCalculator` (mock peso/CEP), `TabeladoFallbackCalculator`, `MotoboySpCalculator` (zona Grande SP)
   - `ShippingService.calcular(req)` agrega as 3 opções; cache Redis `@Cacheable("shipping")` chave `{cep}|{itemsHash}`, TTL 1h
   - V12: schema_shipping (config opcional para tabelado/motoboy — sem dependência runtime nessa fase)
   - 3 opções retornadas: SEDEX (Correios), TABELADO, MOTOBOY (se CEP elegível)

5. `3d38810 feat(checkout): add preview endpoint orchestrating cart, address, shipping and pricing`
   - Módulo `pet-hub-checkout`
   - `CheckoutService.preview(usuarioId, req)`: lê carrinho, valida endereço do owner, calcula frete, valida cupom da request OU do cart, devolve breakdown completo
   - 4xx claros: carrinho vazio, endereço de outro usuário (403), endereço inativo, opção de frete inválida, cupom inválido (422)
   - Sem persistência — chamada idempotente para o frontend mostrar a tela final antes do confirm

6. `2c08f91 feat(checkout): add place-order endpoint with idempotent payment attempt persistence`
   - V13: tabela `tentativas_pagamento` com check constraints em metodo/status, JSONB `response_gateway`, UNIQUE em `idempotency_key`
   - Entidade `TentativaPagamento` (`@Type(JsonType.class)` no JSONB)
   - `placeOrder()` reusa `preview()` para revalidar tudo, checa ownership da cobrança + forma de pagto, cobra via `PaymentGateway`, persiste e limpa o cart no APPROVED
   - Idempotência: 2ª chamada com mesma key retorna o registro existente sem cobrar de novo
   - Referência de pedido gerada `PH-YYYYMMDDHHmmss-XXXX` (o pedido real entra na Fase 4 com BD próprio + máquina de estado)

## ✅ Validações E2E acumuladas (2026-05-14)

- Build (`mvn clean install -DskipTests`) verde, 11 módulos.
- Login Maria → adiciona COLLAR-PRO-001 → preview retorna `subtotal=799.90, frete=39.59, impostos=0, total=839.49`.
- Preview com cupom `10OFF` → `descontoCupom=79.99, total=759.50` (10% sobre subtotal).
- Preview com cupom `EXPIRED` → HTTP 422 "Cupom EXPIRED expirado ou inativo".
- `POST /api/v1/checkout/place-order` (idem=`smoke-test-...`) → `status=APPROVED, tentativaPagamentoId=1`, referência `PH-20260514140256-4753`, transactionId `txn_mock_...`.
- Mesma chamada com mesma idempotency-key → mesmo `tentativaPagamentoId=1` (não cobra de novo).
- Carrinho após APPROVED → vazio (`cartService.limpar()` disparado).
- Linha em `tentativas_pagamento` persistida com `response_gateway` em JSONB legível.

## ⚠️ Dívida técnica restante (não bloqueia Fase 4)

### Pedido completo (fica na Fase 4)
A `TentativaPagamento` é só o registro do pagamento. A Fase 4 acrescenta:
- `Pedido`, `PedidoItem`, `PedidoEvento` (máquina de estados)
- Numeração sequencial por ano (formato `PH-YYYY-NNNNNN`)
- Reservas de estoque (`Estoque`, `Reserva`, `MovimentacaoEstoque`) com `@Scheduled` expirando reservas após 15min
- Trigger pós-APPROVED: confirmar reserva e iniciar máquina de estado em `PEDIDO_CRIADO → PAGAMENTO_APROVADO → SEPARACAO → ...`
- `/orders/{numero}/timeline` com tempo decorrido e baseline

### Gateway real (fica para Fase 7+)
- `MercadoPagoGateway` (sandbox) implementando `PaymentGateway`
- PIX real com QR Code via ZXing (hoje o mock devolve QR mockado por método)
- Boleto PDF (hoje só URL mockada)

### Testes formais
Smoke E2E cobriu happy path + cupom + idempotência + isolamento por endereço. Continuam pendentes (mesmo bloqueio das Fases 1/2 — Testcontainers em container Maven não roteia):
- `CartServiceTest`: adicionar SKU inexistente → 404; atualizar item ausente; clear total via DELETE.
- `PriceCalculatorServiceTest`: combinações cupom percentual + promoção; cupom abaixo do mínimo; impostos por NCM/UF.
- `ShippingServiceTest`: cache hit/miss (verificar 2ª chamada não bate no calculator); CEP fora de SP não devolve motoboy.
- `CheckoutServiceTest`: carrinho vazio → 422; endereço de outro user → 403; idempotency-key reentrante → mesma tentativa; gateway REJECTED → status persistido sem limpar cart.

### Decisões a confirmar
- Mock atual aprova ~90% por RNG. Para a Fase 5 (storefront) é melhor um modo determinístico (cartão `4111` aprova, `4000` recusa) — facilita demo. Decidir antes da Fase 5.
- `parcelas` no `PlaceOrderRequest` é validado entre 1 e 12 mas o mock ignora. Quando entrar Mercado Pago precisa propagar.
- `referenciaPedido` hoje é gerado dentro do CheckoutService. Na Fase 4 quem gera é o `OrderService` (numeração sequencial); o checkout deve passar a chamar o service.

## 🚀 Comando rápido para retomar

```bash
# Verificar estado
cat ai-memory/roadmap/fase-3-pendencias.md
git log --oneline -10

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
# Login Maria (cliente seed):
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"maria.fase2@pethub.com","senha":"Senha@123"}'
```

Swagger: http://localhost:8080/swagger-ui.html

Próximo passo: abrir Fase 4 (Pedidos, Timeline, Estoque) — sem dependência das pendências de teste/JaCoCo.
