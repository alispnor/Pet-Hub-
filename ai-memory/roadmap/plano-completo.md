# 🐾 Pet Hub — Plano de Ação Completo

> **Status:** [approved] — plano canônico do projeto, registrado em 2026-05-11.
> Esta é a fonte de verdade para o roadmap. Atualizações devem ser feitas aqui e propagadas para `ROADMAP.md` (resumo) e demais documentos.

> **Pet Hub** é o e-commerce de produtos tech para pets do **Ali's Pet Ecosystem** — onde o **Pet Diary** registra a vida do seu pet e o **Pet Hub** fornece os produtos inteligentes que fazem essa vida ser melhor.

**Tagline:** *Pet Hub — your one-stop pet tech store* 🛒🐾

---

## 🌐 O Ecossistema

```
┌─────────────────────────┐         ┌─────────────────────────┐
│      🐶  Pet Diary       │ ◄──►   │      🛒  Pet Hub         │
│                         │         │                         │
│  Prontuário do pet:     │         │  E-commerce pet tech:   │
│  • Vacinas              │  Pet    │  • Smart Collars        │
│  • Consultas            │  Data   │  • GPS Trackers         │
│  • Comportamento        │ ──────► │  • Câmeras Pet          │
│  • Crescimento          │ Profile │  • Comedouros IoT       │
│  • Histórico médico     │         │  • Recomendação por IA  │
└─────────────────────────┘         └─────────────────────────┘
        (já existe)                    (este plano de ação)
```

**Sinergia futura:** Pet Diary pode enviar dados do pet (raça, idade, condições) para o Pet Hub gerar recomendações ultra-personalizadas. Em entrevistas, mencione o ecossistema — mostra visão de produto além de código.

---

## 📦 Visão Geral do Pet Hub

### Atores do sistema
1. **Cliente (B2C)** — compra produtos, gerencia pets, endereços, pedidos
2. **Administrador da loja** — gerencia catálogo, estoque, pedidos, descontos, impostos, NF-e
3. **Sistema** — orquestra fluxo via eventos, integra com gateways externos

### Novos requisitos incorporados

#### Área do Cliente
- ✅ Cadastro completo (CPF, dados pessoais, contato)
- ✅ Múltiplos endereços (cobrança e entrega separados)
- ✅ Cadastro de formas de pagamento (cartões tokenizados, PIX, boleto)
- ✅ Cálculo de frete em tempo real (PAC, SEDEX, transportadora)
- ✅ Checkout em etapas (carrinho → endereço → frete → pagamento → revisão → confirmação)
- ✅ Histórico de pedidos com filtros
- ✅ Rastreamento de status (PENDING → PAID → SEPARATED → SHIPPED → DELIVERED)
- ✅ Timeline visual do pedido (igual app de marketplace)
- ✅ Tempo médio de cada etapa exibido

#### Área Administrativa (back-office)
- ✅ Login separado (role ADMIN/MANAGER/OPERATOR)
- ✅ Dashboard com KPIs (vendas, ticket médio, produtos mais vendidos, estoque crítico)
- ✅ CRUD de produtos com upload de imagens
- ✅ Gestão de preços e descontos (cupom, promoção por categoria, flash sale)
- ✅ Controle de estoque (entrada, saída, ajuste, alerta de estoque mínimo)
- ✅ Visualização e gestão de pedidos (mudar status manualmente quando necessário)
- ✅ Relatórios de vendas (por período, categoria, produto, vendedor)
- ✅ Cadastro de regras de impostos (ICMS, IPI, PIS, COFINS por NCM/região)
- ✅ Emissão de NF-e (integração com SEFAZ ou serviço SaaS tipo NFe.io / Focus NFe)

---

## 🗂️ Arquitetura de Microserviços Atualizada

```
pet-hub/
├── backend/
│   ├── api-gateway/              # Spring Cloud Gateway + rate limit
│   ├── identity-service/         # Auth, usuários (cliente + admin), roles
│   ├── catalog-service/          # Produtos, categorias, imagens
│   ├── inventory-service/        # Estoque, reservas, ajustes
│   ├── customer-service/         # Perfil cliente, pets, endereços, payment methods
│   ├── cart-service/             # Carrinho (Redis)
│   ├── pricing-service/          # Preços, descontos, cupons, impostos
│   ├── shipping-service/         # Cálculo de frete + integração Correios/API
│   ├── order-service/            # Pedidos, status, timeline
│   ├── payment-service/          # Integração gateway (Stripe/MercadoPago/PagSeguro mock)
│   ├── invoice-service/          # Emissão NF-e (mock ou Focus NFe sandbox)
│   ├── notification-service/     # Email/push via RabbitMQ
│   ├── recommendation-service/   # IA generativa
│   └── reporting-service/        # Relatórios e analytics admin
├── frontend/
│   ├── storefront/               # Angular - loja para clientes
│   └── admin/                    # Angular - back-office
├── infrastructure/
│   ├── docker/
│   ├── k8s/
│   ├── helm/
│   └── terraform/
└── docs/
```

**Decisão arquitetural importante:** Não é preciso quebrar TUDO de uma vez. Vamos começar com um monolito modular bem estruturado (Fase 1-2), e quebrar em microserviços incrementalmente nas fases seguintes. Isso evita "microservice fatigue" e mostra evolução de arquitetura — o que é mais realista profissionalmente.

---

# 🚀 FASES DO PROJETO V2

## Roadmap em 12 fases

| Fase | Foco | Habilidades |
|------|------|-------------|
| **0** | Bootstrap do monorepo | Git, estrutura |
| **1** | Backend core: catálogo + identidade | Java, Spring Boot, SQL, REST, JWT |
| **2** | Cliente: pets, endereços, formas de pagamento | JPA, validações, criptografia |
| **3** | Carrinho, frete, checkout, pagamento | Integrações, transações |
| **4** | Pedidos, timeline, estoque | Eventos, máquina de estados |
| **5** | Frontend storefront (Angular) | Angular, HTML/CSS/JS |
| **6** | Admin: dashboard, produtos, descontos, estoque, impostos | Angular avançado, gráficos |
| **7** | NF-e e emissão fiscal | Integração XML, certificado digital |
| **8** | Docker | Containerização |
| **9** | Kafka (eventos de domínio) + RabbitMQ (notificações) | Mensageria |
| **10** | IA generativa (recomendações + chatbot) | LLMs, RAG |
| **11** | Kubernetes | K8s manifests, Helm |
| **12** | Cloud + CI/CD + observabilidade | AWS/Oracle, Terraform, Prometheus |

---

# 📝 PROMPTS POR FASE

---

## FASE 0 — Bootstrap do Monorepo

```xml
<role>
Tech lead organizando um monorepo Java + Angular profissional desde o dia zero, com olhar de branding e produto.
</role>

<contexto_projeto>
Nome: **Pet Hub**
Tagline: "Your one-stop pet tech store"
Domínio: e-commerce de produtos tecnológicos para pets (smart collars, GPS trackers, câmeras pet, comedouros IoT, fontes inteligentes, brinquedos interativos)
Ecossistema: faz parte do "Ali's Pet Ecosystem" junto com o projeto irmão **Pet Diary** (prontuário digital de pets, já existente). Futuramente podem integrar.
Público-alvo: tutores de pets que valorizam tecnologia
</contexto_projeto>

<identidade_visual>
Paleta de cores sugerida (use estas mesmas variáveis no Tailwind e SCSS depois):
- Primary:     #6366F1 (indigo-500) — tech, confiança
- Primary dk:  #4F46E5 (indigo-600)
- Accent:      #F59E0B (amber-500) — energia, CTA
- Success:     #10B981 (emerald-500)
- Danger:      #EF4444 (red-500)
- Neutral:     escala slate (50→900)
- Background:  #FFFFFF / dark: #0F172A

Tipografia:
- Primária: Inter (Google Fonts) — interface
- Display:  Poppins (Google Fonts) — títulos/marketing

Logo (conceito): hexágono ou círculo "hub" com pata estilizada no centro. Pode ser texto-only até ter um designer: "**Pet** Hub" com Pet em peso medium slate-900 e Hub em peso bold indigo-500.

Salve as decisões em `docs/brand/BRAND.md` com paleta, fonts, logo guidelines.
</identidade_visual>

<tarefa>
Crie a estrutura inicial do monorepo `pet-hub` com:

1. **README.md raiz** com:
   - Título grande "🐾 Pet Hub" + tagline
   - Badge placeholders (build, coverage, license, version)
   - Seção "About" mencionando o ecossistema Pet Diary + Pet Hub
   - Tech stack (ícones via shields.io)
   - Quick start placeholder
   - Links para ROADMAP.md, ARCHITECTURE.md, BRAND.md, CONTRIBUTING.md
   - Seção "Author" com seu nome (Ali) e links profissionais

2. **ROADMAP.md** (copie o roadmap das 12 fases deste plano)

3. **ARCHITECTURE.md** (esqueleto inicial com seções: Visão Geral, Contexto, Containers, Componentes, Decisões — será expandido por fase)

4. **docs/brand/BRAND.md** com a identidade visual descrita acima

5. **.gitignore** unificado (Java target/, Node node_modules/, IDEs .idea/.vscode, OS .DS_Store, env .env)

6. **.editorconfig** padronizando indentação (Java=4 spaces, JS/TS/HTML/CSS=2 spaces, YAML=2)

7. **LICENSE** (MIT, com seu nome e ano)

8. **CONTRIBUTING.md** com:
   - Conventional Commits (feat:, fix:, docs:, refactor:, test:, chore:)
   - Branch strategy (main + feature/<nome>, hotfix/<nome>)
   - PR template
   - Code style (link pro .editorconfig)

9. **Estrutura de pastas** vazias com .gitkeep:
   ```
   backend/
   frontend/storefront/
   frontend/admin/
   infrastructure/docker/
   infrastructure/k8s/
   infrastructure/helm/
   infrastructure/terraform/
   docs/adr/
   docs/diagrams/
   docs/brand/
   ```

10. **docs/adr/0001-monorepo-vs-multirepo.md** justificando monorepo (facilita devs solo, atomicidade de mudanças cross-stack)

11. **docs/adr/0002-arquitetura-evolutiva.md** explicando: começar monolito modular Java (módulos Maven independentes), evoluir para microserviços nas fases 9+

12. **docs/adr/0003-ecossistema-pet-diary-pet-hub.md** documentando que Pet Hub é parte de um ecossistema maior, com Pet Diary, e como podem integrar no futuro (REST API ou eventos)

13. **Makefile** raiz com placeholders documentados:
    ```makefile
    .PHONY: help build test up down clean
    help:    ## Mostra esta ajuda
    build:   ## Builda todos os projetos
    test:    ## Roda todos os testes
    up:      ## Sobe ambiente local (docker-compose)
    down:    ## Para ambiente local
    clean:   ## Limpa builds
    ```

14. **.github/workflows/ci.yml** básico (apenas estrutura: trigger em push/PR, jobs vazios `lint`, `test`, `build` com `echo "TODO"`)

15. **.github/PULL_REQUEST_TEMPLATE.md** com seções: Descrição, Mudanças, Como testar, Checklist (testes, docs, linter)

16. **.github/ISSUE_TEMPLATE/bug.md** e **feature.md**

17. **CHANGELOG.md** com seção `[Unreleased]` inicial

Use **Conventional Commits desde o primeiro commit**:
- `chore: initial project setup`
- `docs: add roadmap and architecture skeleton`
- `docs: add brand guidelines`

Não inclua código de aplicação ainda — só infraestrutura do repo.

Ao final, faça `git init`, commits separados por escopo, e mostre o `git log` final como prova.
</tarefa>
```

---

## FASE 1 — Backend Core: Catálogo + Identidade

```xml
<role>
Arquiteto Java Senior, especialista em Spring Boot 3 e Clean Architecture, com olhar de segurança (OWASP).
</role>

<projeto>
Backend monolítico modular do Pet Hub. Esta fase entrega:
- Módulo identity: usuários, autenticação, autorização
- Módulo catalog: categorias, produtos, imagens
- Base sólida sobre a qual as próximas fases adicionarão módulos
</projeto>

<stack>
- Java 21, Spring Boot 3.3+
- Maven multi-module (pom pai + módulos)
- Spring Web, Spring Data JPA, Spring Security 6, Spring Validation
- PostgreSQL 16 + Flyway
- Lombok + MapStruct
- SpringDoc OpenAPI 2.x
- JUnit 5, Mockito, AssertJ, Testcontainers
- Logs: SLF4J + Logback com encoder JSON
</stack>

<estrutura_maven>
backend/
├── pom.xml                    (parent, gerencia versões)
├── common/                    (utils compartilhados, exceptions base)
├── identity/                  (módulo)
├── catalog/                   (módulo)
└── application/               (Spring Boot Application + config global)

Cada módulo segue Clean Architecture:
- domain/         entidades, value objects, domain events
- application/    use cases, ports, DTOs
- infrastructure/ adapters: rest, persistence, security
</estrutura_maven>

<modelo_dominio>

### Identity
- **Usuario**: id, nome, email (unique), senhaHash, cpf (unique, opcional inicialmente), telefone, tipoUsuario (CLIENTE/ADMIN), roles (Set<Role>: ROLE_CLIENTE, ROLE_ADMIN_LOJA, ROLE_OPERADOR, ROLE_GERENTE), ativo, dataCadastro, ultimoLogin
- **RefreshToken**: id, usuario, token (hash), expiraEm, revogado

### Catalog
- **Categoria**: id, nome, slug (unique), descricao, categoriaPai (self-ref para subcategorias), ativo, ordem
- **Produto**: id, sku (unique), nome, descricaoCurta, descricaoCompleta, marca, categoria, peso (kg), dimensoes (alturaCm, larguraCm, profundidadeCm), ncm (codigo fiscal 8 dígitos), origem (NACIONAL/IMPORTADO), specs (jsonb), ativo, destacado, dataCadastro
- **ProdutoImagem**: id, produto, url, ordem, principal
- **PrecoVigente**: id, produto, valorBase, dataInicio, dataFim (null = vigente), criadoPor (admin)

NCM é obrigatório para futura emissão de NF-e (Fase 7).
</modelo_dominio>

<endpoints>

### Auth (públicos)
- POST /api/v1/auth/register/cliente — cadastro de cliente final
- POST /api/v1/auth/register/admin — APENAS via admin existente (protegido)
- POST /api/v1/auth/login
- POST /api/v1/auth/refresh
- POST /api/v1/auth/logout
- GET  /api/v1/auth/me

### Catálogo público (storefront)
- GET /api/v1/catalog/categories
- GET /api/v1/catalog/products?categoria={slug}&page=&size=&sort=
- GET /api/v1/catalog/products/{sku}
- GET /api/v1/catalog/products/search?q=

### Catálogo admin
- POST   /api/v1/admin/catalog/categories
- PUT    /api/v1/admin/catalog/categories/{id}
- DELETE /api/v1/admin/catalog/categories/{id}
- POST   /api/v1/admin/catalog/products
- PUT    /api/v1/admin/catalog/products/{id}
- PATCH  /api/v1/admin/catalog/products/{id}/ativo
- POST   /api/v1/admin/catalog/products/{id}/images (multipart)
- DELETE /api/v1/admin/catalog/products/{id}/images/{imgId}
- POST   /api/v1/admin/catalog/products/{id}/price (atualiza preço vigente)

### Health
- GET /actuator/health (público)
- GET /actuator/info, /metrics, /prometheus (autenticado/interno)
</endpoints>

<seguranca>
- JWT access token (15min) + Refresh token (7 dias) armazenado hash em BD
- BCrypt cost 12 para senhas
- Rate limit em /auth/login: 5 tentativas/min/IP (use Bucket4j)
- CORS configurado por env var
- CSP via headers
- Validação rígida de CPF (algoritmo) e email
- Tipos de usuário separados: CLIENTE não pode acessar /admin/**, ADMIN não loga via /auth/login de cliente (use claim no token)
- Senhas NUNCA em logs (configurar masking)
- Endpoints admin exigem role específica via @PreAuthorize
</seguranca>

<padroes_codigo>
Padrão template de função (todas as funções devem seguir):
```java
public Type metodo(Params params) {
    log.debug("Iniciando {} com params={}", "metodo", params);
    validar(params);
    var resultado = executarLogica(params);
    log.debug("Finalizado {} com resultado={}", "metodo", resultado);
    return resultado;
}
```

- Records para DTOs
- MapStruct para conversões entity ↔ DTO
- Exceptions de negócio estendem `BusinessException` (em common/)
- GlobalExceptionHandler retorna RFC 7807 (Problem Details)
- Paginação Spring padrão (Pageable, Page<T>)
- Soft delete onde fizer sentido (campo ativo, não DELETE físico)
</padroes_codigo>

<seed_data>
V1__schema.sql + V2__seed.sql:
- 1 admin: admin@pethub.com / Admin@123 (com role ROLE_ADMIN_LOJA)
- 1 cliente teste: cliente@teste.com / Cliente@123
- 6 categorias: Smart Collars, Comedouros Automáticos, Câmeras Pet, GPS Trackers, Fontes Inteligentes, Brinquedos Interativos
- 20 produtos pet tech variados com NCM realista (ex: 8543.70.99 para dispositivos eletrônicos)
- Preços vigentes
- 3 imagens placeholder por produto (URLs picsum.photos)
</seed_data>

<testes>
- Cobertura > 80% nos services
- @SpringBootTest + Testcontainers para todos os controllers
- Casos: happy path, validação inválida, não autorizado, não encontrado, conflito
- Teste de segurança: cliente tentando acessar admin → 403
</testes>

<processo>
1. Configure parent pom + módulos
2. Crie common (BusinessException, GlobalExceptionHandler, ProblemDetail)
3. Módulo identity primeiro (auth é dependência de tudo)
4. Módulo catalog
5. Migrations + seed
6. Testes
7. README.md de cada módulo + README de backend/
8. Auto-revisão: princípio de menor privilégio, validações, paginação, transações
</processo>
```

---

## FASE 2 — Cliente: Pets, Endereços, Formas de Pagamento

```xml
<role>
Engenheiro Senior especialista em compliance (LGPD, PCI-DSS), com experiência em dados sensíveis (CPF, cartões) e tokenização.
</role>

<contexto>
Backend da Fase 1 funcionando. Vamos adicionar o módulo `customer` para dados completos do cliente: pets (PetHub precisa saber dos pets), endereços (cobrança e entrega) e formas de pagamento tokenizadas.

IMPORTANTE LGPD/PCI:
- NUNCA armazenar PAN (número completo do cartão) nem CVV
- Apenas armazenar token retornado pelo gateway de pagamento + bandeira + 4 últimos dígitos + nome impresso + validade
- CPF armazenado com criptografia em repouso (AES-256-GCM)
- Logs nunca devem expor PII (CPF, email completo) — use masking
</contexto>

<novo_modulo>
backend/customer/

Entidades:
1. **PerfilCliente** (1:1 com Usuario): id, usuario, cpfCriptografado, dataNascimento, genero, telefoneAdicional, aceiteTermos (bool + data), aceiteMarketing
2. **Pet**: id, perfilCliente, nome, especie (CACHORRO/GATO/AVE/PEIXE/REPTIL/OUTROS), raca, dataNascimento, peso (kg), porte (PEQUENO/MEDIO/GRANDE/GIGANTE), observacoes, fotoUrl
3. **Endereco**: id, perfilCliente, apelido (ex: "Casa", "Trabalho"), cep, logradouro, numero, complemento, bairro, cidade, uf (enum), pais (default BR), tipo (RESIDENCIAL/COMERCIAL), padraoEntrega (bool), padraoCobranca (bool), ativo
4. **FormaPagamento**: id, perfilCliente, tipo (CARTAO_CREDITO/CARTAO_DEBITO/PIX/BOLETO), apelido (ex: "Nubank pessoal"), gatewayToken (token do gateway), bandeira (VISA/MASTER/AMEX/ELO/HIPERCARD/OUTRO), ultimosQuatroDigitos, nomeImpresso, validadeMes, validadeAno, padrao (bool), ativo
   - Para PIX e BOLETO, apenas o tipo é armazenado (não há token)
</novo_modulo>

<endpoints>

### Perfil
- GET  /api/v1/customers/me/profile
- PUT  /api/v1/customers/me/profile
- POST /api/v1/customers/me/profile/cpf (separado pois é sensível e único)

### Pets
- GET    /api/v1/customers/me/pets
- POST   /api/v1/customers/me/pets
- GET    /api/v1/customers/me/pets/{id}
- PUT    /api/v1/customers/me/pets/{id}
- DELETE /api/v1/customers/me/pets/{id}
- POST   /api/v1/customers/me/pets/{id}/photo (multipart)

### Endereços
- GET    /api/v1/customers/me/addresses
- POST   /api/v1/customers/me/addresses
- PUT    /api/v1/customers/me/addresses/{id}
- DELETE /api/v1/customers/me/addresses/{id}
- POST   /api/v1/customers/me/addresses/{id}/default (define padrão de entrega/cobrança)
- GET    /api/v1/customers/cep/{cep} — proxy para ViaCEP (cacheia em Redis 24h)

### Formas de pagamento
- GET    /api/v1/customers/me/payment-methods
- POST   /api/v1/customers/me/payment-methods (recebe token do gateway, NÃO o cartão)
- DELETE /api/v1/customers/me/payment-methods/{id}
- POST   /api/v1/customers/me/payment-methods/{id}/default

### Admin (visualizar clientes — sem expor dados sensíveis)
- GET /api/v1/admin/customers?q=&page=&size= (lista com email mascarado)
- GET /api/v1/admin/customers/{id} (detalhes — sem CPF completo, sem cartões)
</endpoints>

<integracoes>
1. **ViaCEP** (https://viacep.com.br/ws/{cep}/json/) para autocomplete de endereço, com fallback se offline
2. **Gateway de pagamento (mock nesta fase)**:
   - Crie interface `PaymentGateway` com método `tokenizeCard(cardData) → token`
   - Implementação `MockPaymentGateway` que valida formato e retorna token fake (`tok_mock_xxxx`)
   - Na Fase 3 substituiremos por gateway real (Mercado Pago sandbox ou Stripe test)
</integracoes>

<criptografia>
- Use Spring Cloud Vault OU envar `APP_ENCRYPTION_KEY` (chave AES 256 base64)
- Crie `EncryptedStringConverter` (AttributeConverter JPA) usando AES-GCM
- Aplique no campo CPF
- Chave NUNCA no código nem em git
</criptografia>

<validacoes>
- CPF: algoritmo dos dígitos verificadores (rejeitar 111.111.111-11 etc)
- CEP: regex 8 dígitos
- Data nascimento: maior de 18 anos (cliente) — configurável
- Telefone: formato brasileiro (validador customizado)
- UF: enum com as 27
</validacoes>

<testes>
- Cobertura > 85% em customer-module
- Teste específico: tentar criar 2 endereços padrão de entrega → apenas o último prevalece (anterior vira não-padrão automaticamente)
- Teste criptografia: salvar e recuperar CPF, validar que no DB está cifrado (via JdbcTemplate raw)
- Teste isolation: cliente A não consegue ver pet/endereço/cartão do cliente B (403)
</testes>

<processo>
1. Configure criptografia + Converter JPA
2. Modele e migre tabelas
3. Implemente endpoints de profile e CPF
4. Pets (mais simples) para validar fluxo
5. Endereços com lógica de padrão
6. Integração ViaCEP com cache Redis (adicione Redis ao docker-compose)
7. Formas de pagamento com gateway mock
8. Testes de segurança e isolamento
9. Auto-revisão LGPD: PII protegida? logs limpos? endpoints autorizados?
</processo>
```

---

## FASE 3 — Carrinho, Frete, Checkout e Pagamento

```xml
<role>
Engenheiro especialista em e-commerce, com experiência em checkout multistep, integrações com Correios/transportadoras e gateways de pagamento (Mercado Pago, Stripe).
</role>

<contexto>
Fluxo de compra completo: carrinho → endereço → frete → pagamento → confirmação.
Esta é a fase mais crítica do e-commerce — precisa ser robusta.
</contexto>

<novos_modulos>
1. **cart-service** (módulo cart/) — carrinho persistido em Redis (TTL 30 dias)
2. **pricing-service** (módulo pricing/) — preços, descontos, cupons, impostos
3. **shipping-service** (módulo shipping/) — cálculo de frete
4. **payment-service** (módulo payment/) — integração gateway
5. **checkout-service** (módulo checkout/) — orquestra o fluxo
</novos_modulos>

<modelos>

### Cart (Redis, não JPA)
```
Key: cart:{userId}
Value JSON: {
  id, userId, items: [{produtoId, sku, nome, imagem, qty, precoUnitario, subtotal}],
  cupom: {codigo, descontoAplicado},
  atualizadoEm
}
```

### Pricing
- **Cupom**: id, codigo (unique), tipo (PERCENTUAL/VALOR_FIXO), valor, valorMinimoCompra, dataInicio, dataFim, usoMaximoTotal, usoMaximoPorCliente, ativo, criadoPor
- **CupomUso**: id, cupom, usuario, pedidoId, usadoEm
- **Promocao**: id, nome, tipo (CATEGORIA/PRODUTO/MARCA/FLASH_SALE), descontoPercentual, dataInicio, dataFim, ativo
- **PromocaoItem**: id, promocao, referenciaId (categoria/produto/marca), tipoReferencia
- **RegraImposto**: id, ncm, ufOrigem, ufDestino, icmsAliquota, ipiAliquota, pisAliquota, cofinsAliquota, vigenciaInicio, vigenciaFim

### Shipping
- **OpcaoFrete** (não persistido, é response): transportadora, servico (PAC/SEDEX/MOTOBOY), valor, prazoDias, codigo
- **FaixaCepRegiao**: id, cepInicio, cepFim, regiao (NORTE/NORDESTE/CO/SUDESTE/SUL)

### Payment
- **TentativaPagamento**: id, pedidoId, valor, formaPagamentoId, gatewayTransactionId, status (PROCESSING/APPROVED/REJECTED/REFUNDED), responseGateway (jsonb), criadoEm, atualizadoEm
</modelos>

<endpoints>

### Cart (autenticado, todos sob /api/v1/cart)
- GET  /              → carrinho atual
- POST /items         → adiciona item {sku, qty}
- PUT  /items/{sku}   → atualiza qty
- DELETE /items/{sku} → remove
- POST /coupon        → aplica cupom {codigo}
- DELETE /coupon      → remove cupom
- DELETE /            → limpa carrinho

### Shipping
- POST /api/v1/shipping/calculate
  Request: { cepDestino, itens: [{sku, qty}] }
  Response: [ OpcaoFrete... ] ordenado por preço

### Checkout
- POST /api/v1/checkout/preview
  Request: { enderecoEntregaId, opcaoFreteCodigo, cupom? }
  Response: {
    subtotal, descontoCupom, descontoPromocoes,
    impostosCalculados: { icms, ipi, pis, cofins, total },
    valorFrete, valorTotal,
    itens: [...]
  }

- POST /api/v1/checkout/place-order
  Request: { enderecoEntregaId, enderecoCobrancaId, opcaoFreteCodigo, formaPagamentoId, cupom?, parcelas? }
  Response: { pedidoId, numeroPedido, status, pagamento: {...} }
  → Cria Pedido, gera evento OrderCreated, inicia processo de pagamento

### Admin Cupons
- POST   /api/v1/admin/coupons
- GET    /api/v1/admin/coupons
- PATCH  /api/v1/admin/coupons/{id}
- DELETE /api/v1/admin/coupons/{id}

### Admin Promoções
- POST /api/v1/admin/promotions ... (CRUD completo)

### Admin Regras de Imposto
- POST /api/v1/admin/tax-rules ... (CRUD completo)
</endpoints>

<calculo_frete>
Implemente `ShippingCalculator` como interface:
- `CorreiosShippingCalculator`: integra com API dos Correios (existe SOAP histórica, mas use uma das alternativas modernas — Frenet API, Melhor Envio API, ou MOCK por enquanto)
- `MotoboyLocalCalculator`: para CEPs de São Paulo capital, mock com R$ 15 fixo, 1 dia
- `TabeladoCalculator`: fallback baseado em FaixaCepRegiao + peso (mock simples)

Estratégia:
1. Calcula peso total + dimensões do pacote (usando dados dos produtos)
2. Tenta Correios; se falhar/timeout, usa Tabelado
3. Se cep de SP capital, adiciona opção Motoboy

Cacheie resultados em Redis por 1h (key: cep+items hash).
</calculo_frete>

<calculo_precos>
Ordem de aplicação no preview:
1. Soma subtotal (preço base × qty)
2. Aplica promoções (maior desconto vence — não acumula)
3. Aplica cupom (sobre subtotal já descontado de promoções)
4. Calcula impostos por NCM/UF (informativo; já embutidos no preço para PF, mas detalhados na NF-e depois)
5. Soma frete
6. Total = (subtotal - descontos) + frete

Implemente `PriceCalculatorService` com método `calculate(cart, address) → PricingBreakdown`.
</calculo_precos>

<integracao_pagamento>
Crie interface `PaymentGateway`:
```java
interface PaymentGateway {
    TokenizationResult tokenizeCard(CardData data);
    PaymentResult charge(ChargeRequest request);
    RefundResult refund(String transactionId, BigDecimal amount);
    PaymentStatus getStatus(String transactionId);
}
```

Implementações:
- `MockPaymentGateway` (default, aprova 90%, rejeita 10% aleatoriamente para testar fluxo)
- `MercadoPagoSandboxGateway` (real, sandbox — use MP SDK, requer credenciais sandbox)

Configurável via property `payment.gateway=mock|mercadopago`.

PIX: gere QR Code (use ZXing); polling de status a cada 5s ou webhook.
Boleto: gere PDF mock com linha digitável.
Cartão: charge síncrono retorna APPROVED/REJECTED.
</integracao_pagamento>

<testes>
- Teste cálculo de preços com matriz de cenários (com/sem cupom, com/sem promoção, combinações)
- Teste cálculo de frete com fallback (Correios offline → tabelado)
- Teste fluxo completo de checkout com payment mock
- Teste cenários negativos: estoque indisponível, cupom expirado, endereço inativo
- Mock externo com WireMock (ViaCEP, Correios, Mercado Pago)
</testes>

<processo>
1. Module cart com Redis
2. Module pricing com cupons/promoções/impostos
3. Module shipping com calculator strategy
4. Module payment com gateway abstrato
5. Module checkout orquestrando tudo
6. Endpoint preview (mais simples, validação)
7. Endpoint place-order (cria pedido — Fase 4 completa o ciclo)
8. Testes
9. Auto-revisão: idempotência? transações? cálculos precisos com BigDecimal?
</processo>
```

---

## FASE 4 — Pedidos, Timeline, Estoque

```xml
<role>
Engenheiro Senior especialista em workflows complexos, máquinas de estado, e gestão de estoque em e-commerce.
</role>

<contexto>
Agora vamos materializar o Pedido criado no checkout, controlar estoque com reservas, e dar visibilidade ao cliente via timeline.
</contexto>

<novos_modulos>
1. **order-service** (módulo order/) — pedido, itens, timeline
2. **inventory-service** (módulo inventory/) — estoque, reservas, movimentações
</novos_modulos>

<modelos>

### Order
- **Pedido**: id, numeroPedido (humano: "PH-2025-000001"), cliente (Usuario), status (enum abaixo), enderecoEntrega (snapshot), enderecoCobranca (snapshot), opcaoFrete (snapshot), valorSubtotal, valorDescontos, valorImpostos, valorFrete, valorTotal, formaPagamentoTipo, formaPagamentoUltimosQuatro, tentativaPagamentoId, criadoEm, atualizadoEm, observacoes, cupomUsado
- **PedidoItem**: id, pedido, produtoId (não FK, snapshot), sku, nomeProduto (snapshot), imagemUrl (snapshot), qty, precoUnitario, descontoUnitario, precoFinal, ncm, impostos (jsonb)
- **PedidoEvento**: id, pedido, tipo (enum), descricao, ocorridoEm, atorTipo (SISTEMA/CLIENTE/ADMIN/GATEWAY), atorId, payload (jsonb)

### Inventory
- **Estoque**: id, produtoId (unique), quantidade, quantidadeReservada, quantidadeMinima (alerta), localizacao (depósito), atualizadoEm
- **MovimentacaoEstoque**: id, produtoId, tipo (ENTRADA/SAIDA/AJUSTE/RESERVA/LIBERACAO_RESERVA/BAIXA_VENDA), quantidade, motivo, pedidoId (opcional), criadoPor, criadoEm
- **ReservaEstoque**: id, produtoId, pedidoId, quantidade, expiraEm (15min para pagamento), status (ATIVA/CONFIRMADA/EXPIRADA/CANCELADA)
</modelos>

<maquina_de_estados_pedido>
Status: PENDENTE_PAGAMENTO → PAGAMENTO_APROVADO → SEPARACAO → EM_TRANSPORTE → ENTREGUE
       ↘ PAGAMENTO_REJEITADO (final)
       ↘ CANCELADO (a qualquer momento antes de EM_TRANSPORTE)
       ↘ DEVOLVIDO (após ENTREGUE)

Implemente como `OrderStateMachine` (use enum + Map de transições válidas).
Cada transição:
1. Valida origem → destino permitido
2. Executa hooks (ex: ao aprovar pagamento → confirma reserva → baixa estoque definitivo)
3. Persiste PedidoEvento
4. Publica evento de domínio (será consumido por outros serviços na Fase 9)
</maquina_de_estados_pedido>

<timeline_para_cliente>
Endpoint GET /api/v1/orders/{numero}/timeline retorna:
```json
{
  "numeroPedido": "PH-2025-000001",
  "statusAtual": "EM_TRANSPORTE",
  "etapas": [
    {"nome": "Pedido recebido", "status": "CONCLUIDA", "ocorridoEm": "...", "tempoDecorrido": "instantâneo"},
    {"nome": "Pagamento aprovado", "status": "CONCLUIDA", "ocorridoEm": "...", "tempoDecorrido": "2 minutos"},
    {"nome": "Em separação", "status": "CONCLUIDA", "ocorridoEm": "...", "tempoDecorrido": "1 dia 3 horas"},
    {"nome": "Em transporte", "status": "ATUAL", "ocorridoEm": "...", "previsaoEntrega": "..."},
    {"nome": "Entregue", "status": "PENDENTE", "previsaoEntrega": "..."}
  ],
  "tempoMedioPorEtapa": { ... },
  "comparativoMedia": "Seu pedido está dentro da média de 3 dias"
}
```
</timeline_para_cliente>

<endpoints>

### Cliente
- GET /api/v1/orders                          → histórico paginado
- GET /api/v1/orders?status=&dataInicio=&dataFim=
- GET /api/v1/orders/{numero}                 → detalhes
- GET /api/v1/orders/{numero}/timeline        → timeline visual
- POST /api/v1/orders/{numero}/cancel         → cliente cancela (se permitido)
- GET /api/v1/orders/{numero}/invoice         → link para NF-e (Fase 7)

### Admin
- GET   /api/v1/admin/orders?status=&q=&page=
- GET   /api/v1/admin/orders/{numero}
- POST  /api/v1/admin/orders/{numero}/transition {paraStatus, observacao}
- GET   /api/v1/admin/orders/stats           → contadores por status

### Inventory
- GET    /api/v1/admin/inventory             → todos os SKUs com estoque
- GET    /api/v1/admin/inventory/low-stock   → abaixo do mínimo
- POST   /api/v1/admin/inventory/movements   → entrada/ajuste manual
- GET    /api/v1/admin/inventory/{sku}/history → movimentações
</endpoints>

<fluxo_reserva_estoque>
1. Checkout chama `inventoryService.reserve(items, pedidoId)`:
   - Para cada item, verifica `quantidade - quantidadeReservada >= qty`
   - Cria `ReservaEstoque` com expiração em 15min
   - Incrementa `quantidadeReservada`
   - Se algum item falhar, rollback de todas reservas, lança exception

2. Pagamento aprovado → `inventoryService.confirmReservation(pedidoId)`:
   - Decrementa `quantidade` e `quantidadeReservada`
   - Cria movimentação BAIXA_VENDA
   - Marca reservas como CONFIRMADAS

3. Pagamento rejeitado/cancelado → `inventoryService.releaseReservation(pedidoId)`:
   - Decrementa `quantidadeReservada`
   - Marca reservas como CANCELADAS

4. Job @Scheduled a cada 1min → libera reservas expiradas:
   - Marca como EXPIRADAS
   - Cancela pedidos PENDENTE_PAGAMENTO há mais de 15min
</fluxo_reserva_estoque>

<numero_pedido>
Gerador `OrderNumberGenerator`:
- Formato: PT-{YYYY}-{6 dígitos zero-padded}
- Sequência por ano (tabela `pedido_sequence`)
- Garantia de unicidade via lock pessimista no incremento

Ano novo reinicia em PH-2026-000001.
</numero_pedido>

<testes>
- Teste máquina de estados: todas as transições válidas + tentativas inválidas
- Teste de concorrência em estoque: 10 threads tentando reservar o último item → apenas 1 sucede
- Teste de expiração de reserva
- Teste de geração de número de pedido (ano novo, concorrência)
- Teste fluxo completo: checkout → pagamento aprovado → estoque baixado → pedido CONFIRMADO
</testes>

<processo>
1. Inventory primeiro (carrinho depende de saber se tem estoque)
2. Order com máquina de estados
3. Integração checkout → orders (cria pedido) → inventory (reserva) → payment (cobra)
4. Hooks de transição de status
5. Timeline com cálculo de tempo decorrido
6. Endpoints admin de gestão
7. Job de expiração
8. Testes extensivos (esta é a parte crítica de e-commerce)
</processo>
```

---

## FASE 5 — Frontend Storefront (Angular)

```xml
<role>
Frontend Senior Angular 17+, especialista em UX de e-commerce, com olhar para conversão (taxa de carrinho abandonado, simplicidade do checkout).
</role>

<contexto>
Construir a loja virtual completa consumindo APIs das Fases 1-4. UX deve ser similar a Amazon/Mercado Livre — familiar e eficiente.
</contexto>

<stack>
- Angular 17+ Standalone Components
- TypeScript strict
- Angular Signals + RxJS
- Lazy loading por feature
- TailwindCSS (justifique no README: rapidez de prototipação + design system flexível)
- Angular Material para componentes complexos (datepicker, autocomplete)
- ngx-mask para CPF/CEP/telefone
- @stripe/stripe-js OU Mercado Pago Bricks para tokenização de cartão (cliente-side, PCI)
- Chart.js para timeline visual do pedido
</stack>

<paginas>

### Públicas
1. **Home (/)** — Hero, banners promocionais, categorias destaque, produtos em destaque, "Pet tech mais buscados"
2. **Catálogo (/produtos)** — filtros (categoria, faixa de preço, marca, avaliação), ordenação, paginação
3. **Detalhe produto (/produtos/:sku)** — galeria, specs, calculadora de frete (informa CEP), botão "Adicionar ao carrinho"
4. **Busca (/busca?q=)** — resultados com mesmos filtros
5. **Login (/login)** + **Cadastro (/cadastro)** — fluxo simples
6. **Recuperar senha (/recuperar-senha)**

### Cliente autenticado
7. **Minha conta (/minha-conta)** — overview com cards: dados pessoais, endereços, cartões, pets, pedidos
8. **Editar perfil (/minha-conta/perfil)**
9. **Meus endereços (/minha-conta/enderecos)** — CRUD com modal
10. **Meus cartões (/minha-conta/cartoes)** — CRUD; ao adicionar, usa Stripe Elements/MP Bricks
11. **Meus pets (/minha-conta/pets)** — CRUD com upload foto
12. **Meus pedidos (/minha-conta/pedidos)** — listagem com filtros (status, período)
13. **Detalhe pedido (/minha-conta/pedidos/:numero)** — TIMELINE VISUAL (super importante), itens, NF-e link

### Checkout (jornada)
14. **Carrinho (/carrinho)** — itens editáveis, cupom, cálculo de frete por CEP, botão "Finalizar compra"
15. **Checkout etapa 1 — Endereço (/checkout/endereco)** — escolher endereço de entrega/cobrança
16. **Checkout etapa 2 — Frete (/checkout/frete)** — escolher opção de envio
17. **Checkout etapa 3 — Pagamento (/checkout/pagamento)** — escolher forma; parcelas se cartão; QR code se PIX
18. **Checkout etapa 4 — Revisão (/checkout/revisao)** — resumo final, botão "Confirmar pedido"
19. **Pedido confirmado (/checkout/sucesso/:numero)** — confete + link para acompanhar
</paginas>

<componentes_reutilizaveis>
- ProductCardComponent
- ProductGridComponent
- PriceDisplayComponent (mostra preço, descontos, parcelamento)
- CartItemComponent
- AddressFormComponent
- CardFormComponent (com Stripe/MP Elements)
- OrderTimelineComponent (TIMELINE VISUAL — destaque do projeto)
- StatusBadgeComponent
- CouponInputComponent
- ShippingCalculatorComponent
</componentes_reutilizaveis>

<timeline_visual>
Componente OrderTimelineComponent deve mostrar:
- Linha horizontal (desktop) ou vertical (mobile)
- Ícones por etapa (cesta, $, caixa, caminhão, casa)
- Estado: concluída (verde), atual (azul pulsante), pendente (cinza)
- Tooltip com data/hora de cada etapa
- Comparação com tempo médio ("3h mais rápido que a média")
- Animação suave entre estados
</timeline_visual>

<estado>
- AuthService (signal de currentUser)
- CartService (signal de carrinho, sincroniza com backend a cada mudança)
- CheckoutService (estado da jornada de checkout, com guards para impedir pular etapas)
- ToastService (notificações)
</estado>

<seguranca_frontend>
- Tokenização de cartão é SEMPRE client-side; o frontend NUNCA envia número/CVV ao backend, apenas o token
- Senha mínima validada no front (8+ chars, maiúscula, número, especial)
- Sanitização de inputs (Angular já protege XSS por padrão, mas valide URLs de imagem antes de exibir)
- Confirmação dupla em ações destrutivas (deletar cartão, cancelar pedido)
</seguranca_frontend>

<acessibilidade_e_seo>
- Semântica HTML correta (header, main, nav, article, section)
- aria-labels em ícones-only
- Contraste WCAG AA
- Skip to content link
- Meta tags por rota (use @angular/ssr OU @ngx-meta/core)
- Open Graph nos produtos para compartilhamento
- Schema.org Product, Offer, AggregateRating
</acessibilidade_e_seo>

<testes>
- Componentes críticos (timeline, card form): testes unitários com Jasmine
- Cypress E2E para o fluxo de checkout completo
</testes>

<entregaveis>
1. Projeto Angular completo
2. Todas as 19 páginas funcionando
3. Componentes reutilizáveis bem isolados
4. Design tokens em tailwind.config.js
5. README com screenshots/GIFs
6. Build de produção otimizada
</entregaveis>

<processo>
1. Scaffold + configuração (Tailwind, ESLint, env)
2. Core (auth, interceptors, guards, services base)
3. Layout (header, footer, sidebar mobile)
4. Páginas públicas (home, catálogo, detalhe)
5. Autenticação (login, cadastro)
6. Área autenticada (conta, pets, endereços, cartões)
7. Carrinho
8. Checkout completo (4 etapas + sucesso)
9. Pedidos + Timeline (componente especial)
10. Polimento, animações, responsivo
11. Acessibilidade e SEO
</processo>
```

---

## FASE 6 — Frontend Admin (Back-Office)

```xml
<role>
Frontend Senior especialista em interfaces administrativas / dashboards, com domínio de Angular, gráficos (Chart.js / ApexCharts), data grids (ag-Grid ou PrimeNG), e UX para usuários power.
</role>

<contexto>
Painel administrativo para gerenciar a loja. Separado do storefront, com seu próprio login e visual mais "denso de informação".
</contexto>

<stack>
- Angular 17+ (mesmo monorepo)
- TailwindCSS + componentes próprios OU PrimeNG (justifique)
- ApexCharts para dashboards
- ag-Grid Community para tabelas grandes
- xlsx para exportar relatórios
- jsPDF para gerar PDFs (relatórios, etiquetas de envio)
</stack>

<paginas>

### Login admin
1. **/admin/login** — separado do storefront, com layout próprio

### Dashboard
2. **/admin** — KPIs principais:
   - Vendas hoje / mês (com comparação ao período anterior)
   - Pedidos por status (gráfico donut)
   - Vendas últimos 30 dias (linha)
   - Top 10 produtos mais vendidos
   - Estoque crítico (alerta)
   - Ticket médio
   - Taxa de conversão (mock)
   - Filtros por período

### Catálogo
3. **/admin/produtos** — listagem com ag-Grid: busca, filtros, edição inline de preço/estoque, ações em massa (ativar/desativar)
4. **/admin/produtos/novo** — formulário completo: dados básicos, descrições, imagens (drag-drop), specs (key-value dinâmico), NCM, dimensões
5. **/admin/produtos/:id/editar** — mesmo formulário
6. **/admin/categorias** — árvore (subcategorias)

### Estoque
7. **/admin/estoque** — tabela com filtros, ação de ajuste manual
8. **/admin/estoque/movimentacoes** — histórico de movimentações
9. **/admin/estoque/alertas** — produtos abaixo do mínimo

### Pedidos
10. **/admin/pedidos** — listagem com filtros avançados (status, período, valor, cliente)
11. **/admin/pedidos/:numero** — detalhes completos + ação de mudar status + observações + reimprimir NF-e + gerar etiqueta de envio

### Descontos e promoções
12. **/admin/cupons** — CRUD
13. **/admin/promocoes** — CRUD com seleção de produtos/categorias

### Fiscal
14. **/admin/fiscal/regras-imposto** — CRUD por NCM e UF
15. **/admin/fiscal/notas** — listagem de NF-e emitidas, filtro, reemissão, cancelamento (Fase 7 completa isso)

### Clientes
16. **/admin/clientes** — listagem, busca, ver detalhes (sem dados sensíveis), histórico de pedidos

### Relatórios
17. **/admin/relatorios/vendas** — por período, categoria, produto, vendedor
18. **/admin/relatorios/estoque** — giro de estoque, ruptura
19. **/admin/relatorios/financeiro** — receita, descontos concedidos, impostos
20. Exportação Excel/PDF em todos os relatórios

### Configurações
21. **/admin/config/usuarios** — usuários admin, roles (apenas GERENTE pode criar outros admins)
22. **/admin/config/loja** — dados da loja para NF-e (CNPJ, IE, regime tributário, endereço)
23. **/admin/config/transportadoras** — habilitar/desabilitar e configurar credenciais
</paginas>

<roles_e_permissoes>
- ROLE_OPERADOR — só visualiza pedidos, atualiza status de envio, ajusta estoque
- ROLE_GERENTE — tudo do operador + CRUD produtos, descontos, ver relatórios
- ROLE_ADMIN_LOJA — tudo + configurações + criar outros admins

Implemente guards por rota e directives `*hasRole="..."` para esconder ações no template.
</roles_e_permissoes>

<dashboard_design>
Layout estilo "Admin Dashboard" moderno:
- Sidebar fixa esquerda colapsável com ícones
- Topbar com busca global, notificações (estoque crítico, pedidos pendentes), avatar
- Cards com sparklines
- Tabelas com sticky headers
- Modais para ações rápidas
- Modo escuro toggle (administrators love it)
</dashboard_design>

<funcionalidades_destaque>

### Drag-and-drop de imagens de produto
Permite reordenar imagens; primeira é a principal.

### Editor inline na tabela de produtos
Editar preço e estoque sem abrir página separada (commit on blur).

### Bulk actions
Selecionar N produtos e: ativar, desativar, aplicar promoção em massa.

### Geração de etiqueta de envio
Botão na tela de pedido → gera PDF com remetente, destinatário, código de rastreamento (mock), código de barras.

### Notificações em tempo real
WebSocket OU Server-Sent Events → quando chega pedido novo, notificação no topbar (sino com badge).

### Audit log
Toda ação admin gera log: quem fez, quando, o que mudou (de → para). Tela /admin/config/auditoria.
</funcionalidades_destaque>

<testes>
- Guards de role (operador NÃO acessa /admin/produtos/novo)
- Componentes críticos (tabela editável, formulário de produto com upload)
- E2E do fluxo: login admin → criar produto → ver no storefront
</testes>

<entregaveis>
1. Projeto Angular admin (frontend/admin/)
2. 23 telas funcionando
3. Roles e permissões aplicadas
4. Dashboard com gráficos reais consumindo APIs de stats
5. Audit log
6. Notificações em tempo real
7. README com screenshots/GIFs do admin

Importante: backend precisa ter endpoints de stats para o dashboard. Se ainda não existirem, adicione:
- GET /api/v1/admin/stats/sales?from=&to=
- GET /api/v1/admin/stats/orders-by-status
- GET /api/v1/admin/stats/top-products
- GET /api/v1/admin/stats/low-stock
</entregaveis>

<processo>
1. Scaffold admin como segundo app Angular no monorepo
2. Layout (sidebar + topbar + outlet)
3. Auth admin separado
4. Dashboard com dados mockados primeiro, depois conecta endpoints
5. CRUD de produtos (mais complexo)
6. Catálogo restante (categorias, estoque)
7. Gestão de pedidos com transições de status
8. Cupons e promoções
9. Relatórios e exportações
10. Audit log e notificações tempo real
11. Roles e refinamento
</processo>
```

---

## FASE 7 — NF-e e Emissão Fiscal

```xml
<role>
Engenheiro Senior com experiência específica em emissão fiscal brasileira (NF-e modelo 55, NFC-e modelo 65), integração SEFAZ, certificado digital A1, e provedores SaaS como Focus NFe, NFe.io, Migrate.
</role>

<contexto>
Pet Hub precisa emitir NF-e para cada venda. Implementação real exige certificado A1 e ambiente homologação SEFAZ, o que é complexo. Vamos usar abordagem dupla:

1. **Modo MOCK (padrão dev)**: gera XML válido estruturalmente + PDF DANFE simulado, sem enviar a SEFAZ
2. **Modo Focus NFe / NFe.io (sandbox)**: integração real com sandbox de provedor SaaS, que abstrai SEFAZ

Configurável via `invoice.provider=mock|focusnfe|nfeio`.
</contexto>

<novo_modulo>
backend/invoice/

Entidades:
1. **NotaFiscal**: id, numero, serie, chaveAcesso (44 dígitos), pedidoId, status (PENDENTE/EMITIDA/REJEITADA/CANCELADA), xmlAssinado (clob), pdfDanfeUrl, dataEmissao, dataAutorizacao, protocoloAutorizacao, motivoRejeicao, valorTotal, providerUsado
2. **NotaFiscalItem**: id, notaFiscal, ordem, produtoCodigo, descricao, ncm, cfop, unidade, qty, valorUnitario, valorTotal, icmsValor, icmsAliquota, icmsCst, ipiValor, pisValor, cofinsValor, origem
3. **DadosEmissor**: id, razaoSocial, nomeFantasia, cnpj, ie, ieSt, im, regimeTributario (SIMPLES/LUCRO_PRESUMIDO/LUCRO_REAL), cnae, endereco (embed), certificadoAlias, ativo
4. **CertificadoDigital** (referência apenas, arquivo .pfx fora do banco): id, alias, caminhoCriptografado, validade, ativo
</novo_modulo>

<endpoints>

### Internos (chamados pelo order-service ao confirmar pagamento)
- POST /api/v1/invoices/issue — { pedidoId } → gera NF-e
- POST /api/v1/invoices/{id}/cancel — { motivo } → cancela (limite 24h após emissão)

### Admin
- GET    /api/v1/admin/invoices?from=&to=&status=
- GET    /api/v1/admin/invoices/{id}
- GET    /api/v1/admin/invoices/{id}/xml      → download XML assinado
- GET    /api/v1/admin/invoices/{id}/danfe    → download PDF DANFE
- POST   /api/v1/admin/invoices/{id}/reissue  → reemite em caso de rejeição
- POST   /api/v1/admin/invoices/{id}/cancel   → cancelamento

### Cliente
- GET /api/v1/orders/{numero}/invoice/xml
- GET /api/v1/orders/{numero}/invoice/danfe
</endpoints>

<fluxo_emissao>
1. Ao pedido transicionar para PAGAMENTO_APROVADO, evento `OrderPaid` é publicado
2. invoice-service consome → constrói payload NF-e
3. Calcula CFOP baseado em: UF emissor vs UF destinatário, tipo de operação (venda)
4. Calcula impostos por item usando RegrasImposto do pricing-service
5. Chama `NFeProvider.issue(payload)`
6. Sucesso → salva NotaFiscal com status EMITIDA, armazena XML e gera DANFE PDF
7. Rejeição → salva com status REJEITADA + motivo, alerta admin
8. Atualiza pedido com referência à NF-e
9. Notifica cliente (email com link para DANFE) — via notification-service

Tudo assíncrono via Kafka (Fase 9 cuida da mensageria; aqui sincrônio basta).
</fluxo_emissao>

<implementacao_mock>
MockNFeProvider:
1. Gera número sequencial por série
2. Constrói chave de acesso fictícia mas com formato correto (UF+AAMM+CNPJ+modelo+serie+numero+tpEmis+codigo+DV)
3. Monta XML conforme schema NFe v4.00 (use biblioteca como `nfe-utils` ou monte com JAXB) — estrutura completa: ide, emit, dest, det, total, transp, pag, infAdic
4. Não assina nem envia a SEFAZ
5. Gera DANFE PDF com biblioteca `nfe-danfe-java` ou monte manualmente com OpenPDF/iText (use template simples mostrando emissor, destinatário, itens, totais, código de barras da chave)
6. Status retorna sempre EMITIDA (ou REJEITADA para SKUs específicos para testar fluxo de erro — ex: produto "TESTE_REJEITAR")
</implementacao_mock>

<implementacao_focus>
FocusNFeProvider:
- Use API REST do Focus NFe sandbox (https://homologacao.focusnfe.com.br)
- Token via variável de ambiente
- POST /v2/nfe?ref={pedidoId} com JSON completo
- GET polling do status (ou webhook se configurado)
- Recupera XML e DANFE da API
</implementacao_focus>

<integracao_admin>
Tela /admin/fiscal/notas funcional:
- Filtros, busca por chave/número
- Reemitir em caso de rejeição
- Cancelar com motivo
- Download XML e DANFE
- Estatísticas: emitidas / rejeitadas / canceladas no período
</integracao_admin>

<testes>
- Geração mock com payload válido (validar com XSD da NF-e v4.00)
- Cálculo de CFOP em todos os cenários (mesma UF, outra UF, exterior)
- Cálculo de impostos por NCM
- Cenário de rejeição (produto TESTE_REJEITAR)
- Cancelamento dentro e fora do prazo
</testes>

<consideracoes>
- Numeração sequencial é REGULAMENTADA: não pode pular números. Em caso de rejeição definitiva, inutilizar o número via API SEFAZ.
- Backup do XML é obrigatório por 5 anos (LC 116/2003).
- Em produção real, certificado A1 precisa de gestão de validade (alertas 30 dias antes).
- Esta implementação é educacional; uso em produção real exige homologação com contador e SEFAZ.
</consideracoes>

<processo>
1. Modelo de dados completo
2. MockNFeProvider primeiro (independe de API externa)
3. Geração de XML estrutural
4. Geração de DANFE PDF
5. Integração com order-service (consumir evento de pedido pago)
6. Endpoints admin
7. (Opcional) FocusNFeProvider com sandbox
8. Frontend admin: tela de notas
9. Documente claramente no README que é educacional
</processo>
```

---

## FASE 8 — Docker

```xml
<role>
DevOps Engineer especialista em containerização de stacks complexas (Java multi-service, Angular multi-app, bancos, message brokers, cache).
</role>

<contexto>
Stack Pet Hub agora tem:
- Backend monolito modular (será quebrado em microserviços na próxima fase, mas por enquanto é um único deployable)
- Frontend storefront (Angular)
- Frontend admin (Angular)
- PostgreSQL
- Redis

Preciso de docker-compose production-grade para rodar tudo local + Dockerfiles otimizados.
</contexto>

<entregaveis>

### Dockerfiles
1. **backend/Dockerfile** — multi-stage com Maven cache, distroless Java 21, healthcheck
2. **frontend/storefront/Dockerfile** — multi-stage Node 20 → Nginx alpine
3. **frontend/admin/Dockerfile** — idem
4. **.dockerignore** em cada (já sabe os padrões)

### nginx.conf customizado para cada frontend
- SPA fallback (try_files ... /index.html)
- gzip + brotli
- Cache de assets versionados (immutable)
- Headers de segurança (CSP estrito, X-Frame-Options, HSTS quando HTTPS)
- Proxy reverso para /api → backend

### docker-compose.yml (raiz)
Serviços:
- postgres:16-alpine — volume nomeado, healthcheck, init script para criar extensão pgvector (para IA na Fase 10)
- redis:7-alpine — para cache + sessões
- backend — build do backend/, depends_on postgres + redis
- storefront — porta 4200
- admin — porta 4201
- mailhog — captura emails locais (porta 8025 UI)
- pgadmin — :5050

### docker-compose.override.yml
Para desenvolvimento local: hot reload, volumes montados, ports debugging (5005 backend JDWP).

### docker-compose.prod.yml
Configurações de produção: sem volumes de código, restart=always, recursos limitados.

### Makefile completo
```makefile
help, up, up-dev, up-prod, down, logs, logs-backend, ps, clean, rebuild,
backend-shell, db-shell, test-backend, test-frontend, lint, format,
build-images, push-images
```

### DOCKER.md
- Pré-requisitos
- Como rodar: `make up`
- URLs: storefront 4200, admin 4201, backend 8080, pgadmin 5050, mailhog 8025
- Troubleshooting
- Como adicionar novo serviço

</entregaveis>

<otimizacoes>
- Backend imagem < 200MB (distroless)
- Frontend imagens < 60MB
- Build cache eficiente (camadas separadas para deps vs código)
- BuildKit habilitado
- Healthchecks de verdade (não só `exit 0`)
- Logs em JSON via Logback
- Sem rodar como root
- Variáveis sensíveis SÓ via .env (.env.example commitado, .env no .gitignore)
</otimizacoes>

<processo>
1. Dockerfiles individuais e teste build
2. docker-compose orquestrando
3. Override de dev com hot reload
4. Override de prod
5. Makefile
6. Documentação
</processo>
```

---

## FASE 9 — Mensageria (Kafka + RabbitMQ)

```xml
<role>
Arquiteto de microserviços event-driven, com domínio profundo de Kafka (eventos de domínio) e RabbitMQ (notificações + tasks).
</role>

<contexto>
Hora de evoluir do monolito modular para arquitetura event-driven. Vamos:
1. Manter os módulos como deployables separados (ou pelo menos extrair order e notification)
2. Usar Kafka para eventos de domínio
3. Usar RabbitMQ para notificações e tasks (envio de email, geração de PDF, etc)
4. Aplicar padrão Outbox para garantia transacional
</contexto>

<arquitetura>
Eventos no Kafka (tópicos):
- `pethub.orders.events` — OrderCreated, OrderPaid, OrderShipped, OrderDelivered, OrderCancelled
- `pethub.inventory.events` — StockReserved, StockReleased, StockBaixaConfirmada
- `pethub.payments.events` — PaymentInitiated, PaymentApproved, PaymentRejected, PaymentRefunded
- `pethub.invoices.events` — InvoiceIssued, InvoiceRejected, InvoiceCancelled

Bridge Kafka → RabbitMQ para notificações:
- Quando OrderPaid → publica em RabbitMQ routing key `notification.email.order.paid`

RabbitMQ exchanges/queues (já detalhado no plano original).
</arquitetura>

[O resto do prompt segue o detalhamento das Fases 4 e 5 do plano V1 — Kafka com Outbox, Avro, schemas, idempotência; RabbitMQ com retry/DLQ, templates de email. Aplicar ao novo contexto onde os eventos cobrem TODO o ciclo de venda, não apenas pedido genérico]

<eventos_importantes_novos>
Adicione além dos eventos do plano V1:

- `InvoiceIssued { pedidoId, invoiceId, chaveAcesso, danfePdfUrl }`
  → consumido por notification-service para enviar email "Sua NF-e está disponível"

- `LowStockDetected { sku, quantidadeAtual, quantidadeMinima }`
  → publicado pelo inventory-service quando detecta produto abaixo do mínimo
  → consumido por notification-service: alerta admin via email + sino no painel

- `OrderShipped { pedidoId, codigoRastreio, transportadora }`
  → notificação ao cliente com link de rastreio
</eventos_importantes_novos>
```

---

## FASE 10 — IA Generativa (Recomendações + Chatbot)

```xml
[Aplica o prompt da Fase 6 do plano V1, com adaptações:]

<contexto_atualizado>
Pet Hub já tem histórico de pedidos, perfis de pets, e dados ricos. A IA agora tem MUITO mais contexto para ser útil:

- Recomendações baseadas em compras anteriores + pets cadastrados
- Chatbot pode responder dúvidas sobre:
  * Status do pedido (consultando order-service via tool/function calling)
  * Política de troca/devolução
  * Compatibilidade de produtos com o pet (raça, idade, porte)
  * Sugestão de produto para problema descrito ("meu cachorro late muito quando saio" → coleira anti-stress, câmera interativa)
</contexto_atualizado>

<funcionalidades_adicionais>

### Geração de descrição de produto (admin)
Ao criar produto, botão "Gerar descrição com IA" que recebe specs + categoria e gera texto comercial.

### Análise de reviews
Job @Scheduled diário:
- Pega reviews novos
- Classifica sentimento (POSITIVO/NEUTRO/NEGATIVO)
- Extrai tags (durabilidade, qualidade, atendimento, etc)
- Detecta reclamações graves → alerta admin

### Resumo de avaliações
Em cada produto, "O que clientes estão dizendo": resumo de 3-4 linhas gerado pela IA a partir de N reviews.

### Function calling no chatbot
Permitir que a IA chame ferramentas:
- `consultar_pedido(numero)` → status do pedido
- `buscar_produtos(query)` → busca catálogo
- `politica_de(topico)` → retorna política específica

Use Spring AI + Anthropic Tools.
</funcionalidades_adicionais>
```

---

## FASE 11 — Kubernetes

```xml
[Aplica o prompt da Fase 7 do plano V1, expandindo para todos os microserviços extraídos]

<servicos_para_k8s>
- api-gateway
- identity-service
- catalog-service
- inventory-service
- customer-service
- cart-service
- pricing-service
- shipping-service
- order-service
- payment-service
- invoice-service
- notification-service
- recommendation-service
- reporting-service
- storefront (Angular)
- admin (Angular)

Stateful:
- postgres (StatefulSet)
- kafka (Strimzi ou bitnami, 3 brokers)
- rabbitmq (cluster 3 nós)
- redis (master-replica)

Cada serviço Java:
- 2-3 réplicas
- HPA por CPU/memória
- PDB
- Probes ajustadas
</servicos_para_k8s>

<helm_chart>
Crie chart umbrella `pethub` que inclui subcharts para cada serviço.
Values.yaml com override por ambiente (dev/staging/prod).
</helm_chart>
```

---

## FASE 12 — Cloud + CI/CD + Observabilidade

```xml
[Aplica os prompts das Fases 8 e 9 do plano V1, com escolha entre AWS e Oracle Cloud]

<adicionar_observabilidade_de_e_commerce>
Métricas de negócio críticas (Micrometer custom):
- pedidos_criados_total (counter)
- pedidos_pagos_total (counter por forma de pagamento)
- pedidos_cancelados_total (counter com motivo)
- valor_vendas_total (counter, BigDecimal → double)
- tempo_pedido_pago_seconds (histogram)
- tempo_separacao_seconds (histogram)
- tempo_entrega_seconds (histogram)
- carrinho_abandonado_total
- frete_calculado_total (por região)
- nfe_emitida_total (counter por status)

Dashboard Grafana "PetHub Business Metrics":
- Funil: visitas → carrinho → checkout → pago → entregue
- Tempo médio em cada etapa
- Top produtos por venda
- Conversão por hora do dia
- Receita por categoria
</adicionar_observabilidade_de_e_commerce>
```

---

# 📅 Cronograma V2 Atualizado

| Fase | Esforço | Notas |
|------|---------|-------|
| 0 — Bootstrap | 2-3h | Rápido |
| 1 — Backend core | 15-20h | Base sólida |
| 2 — Cliente (pets/endereço/pagamento) | 12-15h | Cuidado com LGPD |
| 3 — Checkout (carrinho/frete/pagamento) | 20-25h | **A fase mais crítica** |
| 4 — Pedidos + Estoque | 15-20h | Máquina de estados |
| 5 — Storefront Angular | 25-30h | Muitas telas |
| 6 — Admin Angular | 25-30h | Dashboard pesado |
| 7 — NF-e | 15-20h | Complexidade fiscal |
| 8 — Docker | 4-6h | |
| 9 — Kafka + RabbitMQ | 20-25h | |
| 10 — IA | 12-15h | |
| 11 — Kubernetes | 12-15h | |
| 12 — Cloud + CI/CD + Observability | 15-20h | |

**Total estimado:** ~200-250h de trabalho focado.

---

# 💡 Estratégia de Execução

## Mínimo viável (MVP) para começar a mostrar
**Fases 0 → 5** já dão um e-commerce funcional rodando local. ~80h.
- Já dá pra mostrar em entrevista e impressionar.

## Versão "show de bola" para portfolio público
**Adicionar fases 6, 7, 8, 9.** Total ~160h. Tem admin, NF-e, Docker, eventos.

## Versão completa "vou trabalhar em empresa grande"
**Tudo até a 12.** ~250h. Cloud-native, observabilidade, IA. Compete com qualquer projeto pessoal sênior.

---

# 🎯 Próximos Passos

1. Criar repo `pet-hub` no GitHub
2. Salvar este markdown como `ROADMAP.md`
3. Executar **Fase 0** com Claude Code (rápida, dá o esqueleto)
4. Executar **Fase 1** (backend core)
5. Testar manualmente, commitar, abrir PR
6. Seguir as fases em ordem

**Atalho útil:** mesmo dentro de uma fase, divida o prompt em sub-tarefas se ficar muito longo. Por exemplo, na Fase 1, pode rodar primeiro só o módulo identity, validar, e depois pedir o catalog.

Bom projeto, Ali! 🚀🐾
