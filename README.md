# 🐾 Pet Hub

> Your one-stop pet tech store

Pet Hub é um marketplace de produtos tecnológicos para pets — smart collars, GPS trackers, câmeras pet, comedouros IoT e muito mais — com recomendações personalizadas por IA generativa.

Faz parte do **Ali's Pet Ecosystem** junto com o [Pet Diary](#) (prontuário digital de pets).

![Build](https://img.shields.io/badge/build-passing-brightgreen?style=flat-square)
![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-green?style=flat-square&logo=springboot)
![Angular](https://img.shields.io/badge/Angular-17-red?style=flat-square&logo=angular)
![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)

## ✨ Features

### Para o Cliente
- Catálogo completo de produtos pet tech com filtros e busca
- Cadastro com múltiplos pets, endereços e formas de pagamento
- Checkout completo: carrinho → endereço → frete → pagamento
- Pagamento via cartão, PIX e boleto
- Cálculo de frete em tempo real (Correios + transportadoras)
- Histórico de pedidos com timeline visual de cada etapa
- Recomendações personalizadas por IA baseadas no perfil do pet
- Chatbot assistente para dúvidas sobre produtos e cuidados

### Para o Administrador
- Dashboard com KPIs de vendas, estoque e pedidos
- CRUD de produtos com upload de imagens e specs técnicas
- Gestão de estoque com alertas de mínimo
- Controle de pedidos com transição de status
- Cupons e promoções por produto/categoria
- Relatórios de vendas exportáveis (Excel/PDF)
- Emissão de NF-e (modelo 55)
- Gestão de regras de imposto por NCM/UF
- Audit log de todas as ações administrativas

### Técnico
- Arquitetura event-driven com Kafka e RabbitMQ
- Containerizado com Docker, orquestrado com Kubernetes
- Deploy em cloud pública (AWS/Oracle)
- Observabilidade com Prometheus + Grafana + Loki

## 🎨 Design System

**MANDATORY:** All UI work in this project MUST follow [`ai-memory/design-system.md`](./ai-memory/design-system.md).
Before generating any component, screen, or style:
1. Read `ai-memory/design-system.md` in full
2. Use only tokens defined there (colors, spacing, typography)
3. Reject any UI suggestion that violates the anti-patterns (section 14)

## 🛠️ Tech Stack

**Backend:** Java 21 · Spring Boot 3 · PostgreSQL · Kafka · RabbitMQ · Redis
**Frontend:** Angular 17 · TypeScript · TailwindCSS
**Infrastructure:** Docker · Kubernetes · AWS/Oracle Cloud
**AI:** Anthropic Claude API · Spring AI · PGVector

## 📋 Roadmap

Veja o [ROADMAP.md](./ROADMAP.md) — projeto em 12 fases progressivas.

## 🚀 Quick Start

```bash
# Clone o repositório
git clone https://github.com/alispnor/Pet-Hub-.git pet-hub
cd pet-hub

# Sobe o ambiente completo (a partir da Fase 8)
make up

# Acessa:
# Storefront: http://localhost:4200
# Admin:      http://localhost:4201
# API:        http://localhost:8080/swagger-ui.html
```

> **Status atual:** Fase 0 (Bootstrap). Comandos `make up` e demais entregáveis de runtime aparecem a partir das fases seguintes. Veja [ROADMAP.md](./ROADMAP.md).

## 📚 Documentação

- [ROADMAP.md](./ROADMAP.md) — Plano de desenvolvimento em 12 fases
- [ARCHITECTURE.md](./ARCHITECTURE.md) — Arquitetura do sistema
- [docs/brand/BRAND.md](./docs/brand/BRAND.md) — Identidade visual
- [CONTRIBUTING.md](./CONTRIBUTING.md) — Como contribuir
- [CHANGELOG.md](./CHANGELOG.md) — Histórico de mudanças
- [docs/adr/](./docs/adr/) — Architecture Decision Records

## 🌐 Ecossistema

```
Pet Diary (prontuário) ◄──── Ali's Pet Ecosystem ────► Pet Hub (e-commerce)
```

Futuramente os dois projetos vão integrar: o Pet Diary envia o perfil do pet para o Pet Hub gerar recomendações ultra-personalizadas.

## 👤 Autor

**Ali** — Senior Full Stack Developer
[GitHub](https://github.com/alispnor)

## 📄 Licença

MIT © 2026 Ali
