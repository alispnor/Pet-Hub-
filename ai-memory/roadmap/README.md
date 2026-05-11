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

## Convenções de naming notadas

Durante o planejamento o projeto recebeu vários nomes — todos referem ao mesmo produto:

- **Pet Hub** (oficial atual, prefixo de pedido `PH-`) ← usar este
- **Pet-Hub Store** (variante intermediária)
- **PetHub** / **pethub** (em código, slugs, configs)

Quando escrever código, usar `pethub` em paths/packages (`com.alispnor.pethub`), `pet-hub` em diretórios (`~/projects/pet-hub`), e "Pet Hub" em prosa.
