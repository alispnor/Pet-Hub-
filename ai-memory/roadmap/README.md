# ai-memory/roadmap

Documentos de planejamento e execução do projeto Pet Hub.

## Hierarquia (qual usar quando)

```
plano-completo.md              ← FONTE DE VERDADE (V2, 12 fases, prefixo PH-)
├── plano-v1-portfolio-9-fases.md   ← versão antiga (9 fases, framing portfolio) [superseded]
├── prompt-execucao-fase-0-1.md     ← prompt operacional pronto para colar (Fase 0 + 1 juntas)
└── prompt-ativacao-sessao.md       ← bloco a colar no INÍCIO de cada sessão (skills + contexto)
```

## Arquivos

| Arquivo | O que é | Quando usar |
|---|---|---|
| [plano-completo.md](plano-completo.md) | Plano estratégico V2 — 12 fases detalhadas com prompts XML por fase, modelo de domínio, endpoints, padrões, cronograma, estratégia de execução (MVP / portfolio / completo) | Referência principal. Consultar antes de iniciar qualquer fase, ou quando precisar entender o escopo geral. |
| [plano-v1-portfolio-9-fases.md](plano-v1-portfolio-9-fases.md) | Plano original V1 — 9 fases, framing portfolio (checklist de skills para vaga Senior Full Stack) | Apenas histórico. Substituído por V2 (e-commerce completo com NF-e e back-office). Útil só para entender a evolução do escopo. |
| [prompt-execucao-fase-0-1.md](prompt-execucao-fase-0-1.md) | Prompt operacional **pronto para colar no Claude Code** executando Fases 0 (bootstrap) e 1 (backend core) numa sessão. Especifica entidades, DTOs, endpoints, migrations, checklist, padrão de commits | Colar quando for iniciar o desenvolvimento de fato. Mais concreto que o plano estratégico. |
| [prompt-ativacao-sessao.md](prompt-ativacao-sessao.md) | Bloco de contexto para colar **antes** do prompt de cada fase: ativa skills locais, plugins (context7, frontend-design), lembra projetos anteriores e padrões do Ali | Colar no início de cada sessão nova de execução. Substituir `Fase atual: [N]` antes de usar. |
| [fase-1-pendencias.md](fase-1-pendencias.md) | **Pendências da Fase 1** (sessão pausada em 2026-05-11): checklist do que foi entregue, riscos a verificar quando rodar `mvn clean install`, testes faltantes, onde retomar | Ler primeiro ao iniciar a próxima sessão de Fase 1. Apagar quando a Fase 1 for marcada como ✅. |
| [fase-2-pendencias.md](fase-2-pendencias.md) | **Histórico da Fase 2** (✅ entregue em 2026-05-13): 7 commits do módulo customer (perfil + pets + endereços + formas de pagamento + admin com PII mascarada), validações E2E acumuladas, dívida de testes Mockito + Testcontainers. | Referência ao revisitar customer; pendências de teste seguem para fechar junto com as demais. |
| [fase-3-pendencias.md](fase-3-pendencias.md) | **Histórico da Fase 3** (✅ entregue em 2026-05-14): 6 commits do fluxo de compra (payment gateway extraído, cart Redis 30d, pricing com cupons/promoções/impostos, shipping 3 calculadoras + cache 1h, checkout preview, place-order idempotente). Notas sobre o que fica para Fase 4 (Pedido + máquina de estados + estoque). | Referência ao revisitar checkout/pricing/shipping/cart. |
| [fase-4-pendencias.md](fase-4-pendencias.md) | **Histórico da Fase 4** (✅ entregue em 2026-05-14): 2 commits (inventory com reservas TTL 15min + order com state machine + checkout wired). MockPaymentGateway determinístico. Notas sobre cancelamento pós-aprovado, webhook assíncrono, testes formais. | Referência ao revisitar inventory/order/state machine. |

## Convenções de naming notadas

Durante o planejamento o projeto recebeu vários nomes — todos referem ao mesmo produto:

- **Pet Hub** (oficial atual, prefixo de pedido `PH-`) ← usar este
- **Pet-Hub Store** (variante intermediária)
- **PetHub** / **pethub** (em código, slugs, configs)

Quando escrever código, usar `pethub` em paths/packages (`com.alispnor.pethub`), `pet-hub` em diretórios (`~/projects/pet-hub`), e "Pet Hub" em prosa.
