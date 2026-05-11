# Pet Hub — Makefile
#
# Targets placeholder na Fase 0. Implementação real entra na Fase 1
# (backend-shell, db-shell, test-backend) e Fase 8 (up, down, logs)
# quando o docker-compose existir de fato.
#
# Convenção: cada target tem um comentário "## descrição" parseado
# pelo `make help` para listagem amigável.

COMPOSE_FILE := infrastructure/docker/docker-compose.dev.yml
COMPOSE      := docker compose -f $(COMPOSE_FILE)

.DEFAULT_GOAL := help

.PHONY: help build test up down clean logs ps rebuild backend-shell db-shell test-backend test-frontend lint format

help: ## Mostra esta ajuda
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

# ─── Build & Test ──────────────────────────────────────────────

build: ## Builda todos os projetos (placeholder até Fase 1)
	@echo "TODO Fase 1+: cd backend && mvn clean package -DskipTests"
	@echo "TODO Fase 5+: cd frontend/storefront && npm run build"
	@echo "TODO Fase 6+: cd frontend/admin && npm run build"

test: test-backend test-frontend ## Roda todos os testes

test-backend: ## Testes do backend (placeholder até Fase 1)
	@echo "TODO Fase 1+: cd backend && mvn test"

test-frontend: ## Testes dos frontends (placeholder até Fase 5)
	@echo "TODO Fase 5+: cd frontend/storefront && npm test"
	@echo "TODO Fase 6+: cd frontend/admin && npm test"

lint: ## Lint em todos os projetos (placeholder)
	@echo "TODO Fase 1+: cd backend && mvn spotless:check"
	@echo "TODO Fase 5+: cd frontend/storefront && npm run lint"
	@echo "TODO Fase 6+: cd frontend/admin && npm run lint"

format: ## Formata todos os projetos (placeholder)
	@echo "TODO Fase 1+: cd backend && mvn spotless:apply"
	@echo "TODO Fase 5+: cd frontend/storefront && npm run format"

# ─── Ambiente local (docker compose) ──────────────────────────

up: ## Sobe ambiente local (placeholder até Fase 8)
	@test -f $(COMPOSE_FILE) || (echo "❌ $(COMPOSE_FILE) não existe ainda — entregue na Fase 1/8"; exit 1)
	$(COMPOSE) up -d --build

down: ## Para ambiente local
	@test -f $(COMPOSE_FILE) || (echo "ℹ️ Nada para parar — compose ainda não existe"; exit 0)
	$(COMPOSE) down

clean: ## Para ambiente e remove volumes (apaga dados locais)
	@test -f $(COMPOSE_FILE) || (echo "ℹ️ Nada para limpar"; exit 0)
	$(COMPOSE) down -v

logs: ## Tail dos logs de todos os serviços
	$(COMPOSE) logs -f

ps: ## Lista os containers do projeto
	$(COMPOSE) ps

rebuild: down up ## Down + up --build (atalho)

# ─── Shells de debug ──────────────────────────────────────────

backend-shell: ## Shell dentro do container do backend (Fase 1+)
	@echo "TODO Fase 1+: $(COMPOSE) exec backend sh"

db-shell: ## psql no Postgres (Fase 1+)
	@echo "TODO Fase 1+: $(COMPOSE) exec postgres psql -U pethub -d pethub"
