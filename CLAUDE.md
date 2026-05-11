# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Status

**Phase 0 — Bootstrap.** The repository currently contains only a README, LICENSE, and .gitignore. No application code, build files, or directory structure exist yet. When the user asks to scaffold features, the implementation has to be created from scratch — there are no existing patterns to follow in this repo.

The user (Ali) communicates in Portuguese (Brazilian). Default to Portuguese for explanations and discussion unless the user switches to English; code, identifiers, and commit messages stay in English.

## Product Context

**Pet Hub** is an e-commerce for pet tech products (smart collars, GPS trackers, IoT feeders, etc.) with AI-driven personalized recommendations. It is one of two apps in **Ali's Pet Ecosystem** — the sibling app is **Pet Diary** (digital pet medical record). Cross-app integration may come up; treat Pet Diary as a separate repository.

## Intended Architecture

The README commits to this stack (no code yet — these are the targets):

- **Backend:** Java 21 + Spring Boot 3, PostgreSQL, Kafka, RabbitMQ, Redis
- **Frontend:** Angular 17+, TypeScript, TailwindCSS
- **Infra:** Docker, Kubernetes, AWS / Oracle Cloud
- **AI:** Anthropic Claude API for recommendations and chatbot

When scaffolding, anchor choices to these — don't substitute (e.g., don't pick React or Node for the backend) without explicit confirmation from the user.

The split between Kafka and RabbitMQ is intentional in the stated stack; before adding messaging code, ask the user which broker owns which flows so the boundary is decided once rather than per-feature.

## Roadmap

Top-level `ROADMAP.md` has the 12-phase summary table. The detailed plan (prompts per phase, domain models, endpoints, patterns) lives in `ai-memory/roadmap/plano-completo.md` — **that is the source of truth for scope**. Two operational artifacts in the same folder are also relevant:

- `ai-memory/roadmap/prompt-execucao-fase-0-1.md` — the concrete prompt Ali plans to paste into Claude Code to execute Phases 0+1 in one session. Use it as the canonical spec when he asks to start implementation.
- `ai-memory/roadmap/prompt-ativacao-sessao.md` — bootstrap context block (skills/plugins/patterns) Ali pastes at the start of each session.

When Ali references a phase, consult `plano-completo.md` first. If a sub-detail is missing there, check the V1 plan (`plano-v1-portfolio-9-fases.md`, marked superseded but useful for context).

## Coding patterns Ali has already declared

Apply without asking — these are invariants for the project:
- Function template: `log entry → validate → logic → log exit → return` (`@Slf4j`).
- DTOs as Java records (immutable).
- MapStruct for all entity ↔ DTO conversions (never manual).
- Clean Architecture per module: `domain/`, `application/`, `infrastructure/`.
- Exceptions extend `BusinessException` (in `common/`); `GlobalExceptionHandler` returns RFC 7807.
- No business logic in controllers.
- Conventional Commits with module scope: `feat(catalog):`, `feat(identity):`, etc.

Full rationale in `~/.claude/projects/-home-ali-projects-pet-hub/memory/feedback_coding_patterns.md`.

## Project Knowledge Base — `ai-memory/`

Long-form project context (architectural decisions, domain notes, integration specs, roadmap details) lives in `ai-memory/` at the repo root, organized into `decisions/`, `domain/`, `architecture/`, `integrations/`, `roadmap/`, `notes/`. See `ai-memory/README.md` for the convention.

When the user asks you to **remember**, **save**, or **document** something about the project that doesn't fit in code, write it under `ai-memory/` (not your private memory store) so it lives with the repo and is shared with the team.
