# ADR 0002 — Arquitetura Evolutiva: Monolito Modular → Microserviços

**Status:** Aceita
**Data:** 2026-05-11
**Decisão por:** Ali

## Contexto

Pet Hub é um projeto sênior de portfolio que precisa demonstrar competência em microserviços, Kafka, Kubernetes e arquiteturas cloud-native. A pressão inicial é "começar microserviços desde o dia 1" para mostrar a skill.

Mas há um trade-off:

- **Microserviços desde o início** força decisões de boundary antes de entender o domínio direito, e adiciona muita complexidade operacional (gateway, service discovery, distributed tracing, etc.) quando ainda não há funcionalidade.
- **Monolito modular** entrega valor rápido com complexidade baixa, mas pode parecer "menos impressionante" em portfolio.

## Decisão

**Começar como monolito modular Maven**, e extrair microserviços incrementalmente a partir da **Fase 9**.

**Estrutura modular desde o início:**
```
backend/
├── pom.xml          (parent)
├── common/          (utils, exceptions)
├── identity/        (módulo)
├── catalog/         (módulo)
├── customer/        (módulo, Fase 2)
├── cart/            (módulo, Fase 3)
├── ...
└── application/     (Spring Boot Application — único deployable até Fase 9)
```

Cada módulo segue Clean Architecture (domain/application/infrastructure) e tem **dependências explícitas** declaradas no `pom.xml`. Isso significa que **transformar um módulo em microserviço depois é mecânico** — o boundary já está claro.

**A partir da Fase 9:**
- Extração de `order` e `notification` como deployables separados.
- Kafka como event bus para comunicação assíncrona.
- Outros módulos podem ser extraídos sob demanda, conforme necessidade real (escala, time, deploy independente).

## Razões

- **Aprendizado realista.** A maioria dos sistemas reais nasce monolito e evolui. Esse arco é o que se vive no mercado.
- **Menor risco de over-engineering.** Microserviços antes de o domínio estar maduro produz boundaries errados que custam caro para mudar.
- **Modularidade desde o início garante a evolução.** Como cada módulo já tem boundary claro, a Fase 9 vira um exercício de empacotamento, não de refactor estrutural.
- **Portfolio cobre os dois lados.** Demonstra capacidade de construir um monolito limpo **e** de evoluir para microserviços — habilidade mais valiosa que só uma das duas.
- **Velocidade.** Fases 1–8 entregam rápido sem ter que orquestrar 5+ serviços e brokers desde o início.

## Consequências

- ✅ Fases 1–8 progridem rapidamente sem complexidade operacional de microserviços.
- ✅ Boundaries de módulo viram boundaries de serviço naturalmente.
- ✅ Banco compartilhado (uma instância Postgres com schemas separados ou apenas tabelas com prefixo) é OK enquanto monolito; cada microserviço extraído ganha seu próprio banco.
- ✅ O usuário vê evolução arquitetural no histórico do projeto — narrativa forte para entrevistas.
- ⚠️ Tentação de "código entre módulos sem passar por API" — disciplina necessária: módulos só se comunicam via interfaces da camada `application/port/`.
- ⚠️ Antes da Fase 9, eventos são síncronos (chamadas diretas entre services). A transição para Kafka requer cuidado para preservar comportamento.

## Critérios para extrair um módulo como microserviço

Não é uma regra rígida, mas pelo menos um dos seguintes deve aplicar:

1. **Diferentes requisitos de escala** (ex.: `recommendation` consome muito CPU; isolá-lo evita afetar o resto).
2. **Diferentes ciclos de deploy** (ex.: `notification` muda templates frequentemente; deploy isolado evita derrubar o checkout).
3. **Diferentes domínios de proprietário** (não aplicável em dev solo, mas mantido por completude).
4. **Stack diferente** (ex.: `recommendation` precisa de PGVector + Spring AI; pode justificar separação).

## Alternativas consideradas

**Microserviços desde a Fase 1** rejeitado:
- Mata produtividade nas fases iniciais com infra antes do domínio.
- Boundaries quase certamente errados (não conhecemos o domínio ainda).
- Mais difícil de testar (saga distribuída desde o dia 1).

**Monolito sem modularização** rejeitado:
- Pet Hub é grande o bastante para ter módulos. Sem boundary, vira big ball of mud.
- Modularidade Maven é "grátis" — já segue padrão Spring Boot multi-module.

## Referências

- [Monolith First — Martin Fowler](https://martinfowler.com/bliki/MonolithFirst.html)
- [Modular Monolith — Kamil Grzybek](https://www.kamilgrzybek.com/design/modular-monolith-primer/)
- [Don't Start with Microservices — Stefan Tilkov](https://martinfowler.com/articles/dont-start-monolith.html)
