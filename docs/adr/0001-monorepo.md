# ADR 0001 — Monorepo vs Multi-repo

**Status:** Aceita
**Data:** 2026-05-11
**Decisão por:** Ali

## Contexto

Pet Hub envolve múltiplos artefatos: backend Java (Spring Boot multi-module), frontends Angular (storefront + admin), infraestrutura (Docker, Kubernetes, Helm, Terraform), documentação técnica, ADRs e diagramas.

Há duas estruturas viáveis:

1. **Monorepo:** todos os artefatos em um único repositório (`pet-hub/`).
2. **Multi-repo:** um repositório por artefato (`pet-hub-backend`, `pet-hub-storefront`, `pet-hub-admin`, `pet-hub-infrastructure`, etc.).

## Decisão

Adotamos **Monorepo** (`github.com/alispnor/Pet-Hub-`).

## Razões

- **Dev solo.** Pet Hub é um projeto pessoal/portfolio. Multi-repo otimiza para muitos times trabalhando em paralelo; monorepo otimiza para uma pessoa fazendo mudanças coordenadas entre camadas.
- **Atomicidade cross-stack.** Uma mudança que afeta API + storefront pode ser feita em um único PR, com um único histórico de mudança. Sem PR-pingue-pongue entre repos.
- **Refactor mais fácil.** Renomear um campo na API e propagar no frontend é uma busca-e-substitui no repo todo.
- **Documentação centralizada.** ROADMAP, ARCHITECTURE, ADRs, BRAND ficam num lugar só e versionados junto com o código.
- **CI/CD simplificado.** Um único `.github/workflows/` controla pipelines. Caminhos diferentes (`backend/**`, `frontend/**`) podem disparar jobs específicos via `paths:` filters quando isso for necessário.

## Consequências

- ✅ Mudanças coordenadas entre backend, frontend e infra ficam triviais.
- ✅ Onboarding (eu mesmo daqui a 6 meses) lê **um** README e entende o projeto.
- ✅ Versionamento conjunto via tags (`v1.0.0` cobre tudo) ou independente por scope nos Conventional Commits.
- ⚠️ O CI precisa ser inteligente: roda build do backend só quando `backend/**` muda, etc. Resolvido nas Fases 1 e 8.
- ⚠️ Se o projeto crescer muito (>100k LOC, >5 devs), pode haver pressão para split. Cruzaremos essa ponte se chegarmos lá.

## Alternativas consideradas

**Multi-repo** rejeitado porque:
- Multiplica overhead operacional (vários `git clone`, várias chaves SSH, múltiplos CIs configurados).
- Dificulta refactors cross-cutting.
- Para um dev solo, o ganho de "isolamento" é teórico — eu sou o único dono de tudo.

## Referências

- [Monorepos: Please don't! por Matt Klein](https://medium.com/@mattklein123/monorepos-please-dont-e9a279be011b) — argumentos contra (úteis para entender quando o monorepo dói).
- [Google's Trunk-Based Development](https://trunkbaseddevelopment.com/) — exemplo de monorepo em escala extrema, mostra que funciona bem com tooling certo.
