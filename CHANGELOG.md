# Changelog

Todas as mudanças notáveis neste projeto são documentadas neste arquivo.

O formato é baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/),
e este projeto adere a [Versionamento Semântico](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Adicionado
- `README.md` expandido com features (Cliente/Admin/Técnico), badges, quick start e bloco de ecossistema.
- `ROADMAP.md` detalhado com objetivos, entregáveis, habilidades e esforço por fase (12 fases).
- `ARCHITECTURE.md` com visão geral, diagrama de contexto C4 e padrões adotados.
- `docs/brand/BRAND.md` com paleta de cores, tipografia, logo, iconografia e tom de voz.
- `docs/adr/0001-monorepo.md` — decisão por monorepo vs multi-repo.
- `docs/adr/0002-arquitetura-evolutiva.md` — monolito modular → microserviços a partir da Fase 9.
- `docs/adr/0003-pet-ecosystem.md` — relação entre Pet Hub e Pet Diary.
- `CONTRIBUTING.md` com Conventional Commits, branch strategy, PR workflow e code style.
- `Makefile` com targets de desenvolvimento (placeholders apontando para fases futuras).
- `.gitignore` unificado cobrindo Java, Node, IDEs, OS, env, build e infraestrutura.
- `.editorconfig` padronizando indentação por tipo de arquivo.
- Estrutura de pastas `backend/`, `frontend/{storefront,admin}/`, `infrastructure/{docker,k8s,helm,terraform}/`, `docs/{adr,diagrams,brand}/`.
- `.github/workflows/ci.yml` placeholder com jobs `lint`, `test`, `build`.
- Templates de PR e de issue (bug report + feature request) em `.github/`.

### Mudado
- `README.md` reescrito (antes: 26 linhas mínimas; agora: ~100 linhas com landing page completa).
- `ROADMAP.md` reescrito (antes: tabela resumo; agora: detalhamento por fase com habilidades demonstradas).
- `.gitignore` reescrito (antes: básico; agora: cobertura completa do stack).

### Mantido sem alteração
- `LICENSE` — MIT, já existente.
- `CLAUDE.md` — guia do Claude Code, adicionado em sessão anterior.
- `ai-memory/` — base de conhecimento com plano completo das 12 fases.

---

## [0.0.0] — 2026-05-11

### Adicionado
- Setup inicial do repositório: README mínimo, LICENSE MIT, .gitignore básico.
- `CLAUDE.md` guiando Claude Code neste repositório.
- `ai-memory/` com plano completo (V2, 12 fases), plano histórico (V1, 9 fases), prompts de execução e ativação de sessão.

[Unreleased]: https://github.com/alispnor/Pet-Hub-/compare/v0.0.0...HEAD
[0.0.0]: https://github.com/alispnor/Pet-Hub-/releases/tag/v0.0.0
