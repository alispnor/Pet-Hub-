# 🧠 Pet Hub — Prompt de Ativação de Sessão (skills + plugins + contexto)

> **Status:** [approved] — bloco para colar no INÍCIO de cada sessão do Claude Code, ANTES do prompt principal da fase.
> **Propósito:** ativar skills/plugins globais da máquina do Ali, lembrar padrões de projetos anteriores, e injetar a identidade visual + stack do Pet Hub.

> Este bloco ativa todas as skills, plugins e contexto de projetos anteriores.
> Cole ANTES do prompt principal de cada fase.

---

## PROMPT DE ATIVAÇÃO — Cola no Claude Code antes de qualquer fase

```
<machine_context>
Você está rodando na máquina do Ali, Senior Full Stack Developer.
Esta máquina tem skills e plugins globais instalados que você DEVE usar ativamente.
Leia, aplique e mencione quando estiver usando cada um.
</machine_context>

<skills_disponiveis>
As seguintes skills estão instaladas em ~/.claude/skills/ e ~/.agents/skills/.
Você TEM ACESSO a elas. Leia o SKILL.md de cada uma antes de gerar código frontend.

Skills de DESIGN e UI (use para todo código Angular/HTML/CSS):
- frontend-design       → padrões de componentes Angular production-grade
- high-end-visual-design → UI com acabamento premium, não genérico
- web-design-guidelines → boas práticas de layout, espaçamento, tipografia
- make-interfaces-feel-better → microinterações, animações, UX polish
- minimalist-ui         → design limpo, sem excesso
- brandkit              → consistência de marca e design tokens
- design-taste-frontend → decisões de gosto visual elevado
- image-to-code         → converter referências visuais em código
- redesign-existing-projects → melhorar interfaces existentes

Skills de CÓDIGO Angular/React (use para estrutura e padrões):
- vercel-react-best-practices     → padrões modernos de componentes
- vercel-composition-patterns     → composição de componentes reutilizáveis
- vercel-react-view-transitions   → transições de página suaves

Regra: ANTES de gerar qualquer componente Angular ou tela HTML/CSS,
leia o SKILL.md da skill relevante e aplique as diretrizes.
Não gere UI genérica — use o conhecimento das skills para produzir
interfaces com qualidade de produto real.
</skills_disponiveis>

<plugins_ativos>
Os seguintes plugins globais estão habilitados nesta sessão:

1. frontend-design (@claude-plugins-official)
   → Ativo para geração de componentes Angular/HTML/CSS
   → Aplica design system consistente automaticamente
   → Use para TODAS as telas do storefront e admin

2. superpowers (@claude-plugins-official)
   → Capacidades expandidas de geração de código
   → Use para tarefas complexas multi-arquivo

3. context7 (@claude-plugins-official) + MCP context7
   → Acessa documentação atualizada de libs via npx -y @upstash/context7-mcp
   → Use SEMPRE antes de gerar código com libs que podem ter mudado:
     Angular 17+, Spring Boot 3.3+, Spring Security 6, jjwt 0.12.x,
     Testcontainers, MapStruct 1.5+, TailwindCSS v3
   → Comando: consulte context7 para confirmar APIs atualizadas antes de usar
</plugins_ativos>

<projetos_anteriores>
Ali tem experiência com os seguintes projetos que você pode usar como referência
de padrões, decisões técnicas e lições aprendidas:

Frontend (Angular/Vue/React):
- Cellar Vinhos: frontend Livewire + Alpine.js com Redis e Kafka
- Vue.js 2.6.12 multilingual com global components
- React JS e Vue JS em projetos de produção na Guep Technology

Backend (PHP/Laravel/Python/Django):
- Laravel com Kafka integration e caching (Redis)
- Python/Django microservice com Clean Architecture, SOLID, Kafka, Docker, PostgreSQL
- Python XML Processing Service (large XML → JSON → Oracle DB via SSH)
- GatewayHandler + XMLParser integration
- Redis Connection Manager para múltiplos serviços
- Livewire CRUD ticketing system

Padrões estabelecidos que Ali prefere (SEMPRE aplicar):
1. Template de função: log entrada → validação → lógica → log saída → retorno
2. Código reutilizável e padronizado entre arquivos similares
3. Sistemas eficientes com baixo uso de recursos
4. Simplicidade e escalabilidade acima de overengineering
5. DTOs como records Java (imutáveis)
6. MapStruct para conversões (nunca manual)
7. Clean Architecture / Hexagonal em todos os módulos backend
8. Conventional Commits em todos os commits git
</projetos_anteriores>

<projeto_atual>
Nome: Pet Hub
Tipo: E-commerce de produtos tech para pets
Parte do: Ali's Pet Ecosystem (junto com Pet Diary — prontuário digital de pets)
Tagline: "Your one-stop pet tech store"

Stack:
- Backend: Java 21, Spring Boot 3.3+, Maven multi-module, PostgreSQL 16
- Frontend: Angular 17+ Standalone Components, TypeScript strict, TailwindCSS
- Mensageria: Kafka + RabbitMQ
- Cache: Redis
- Container: Docker + Kubernetes
- Cloud: AWS ou Oracle Cloud
- IA: Anthropic Claude API + Spring AI + PGVector

Identidade visual:
- Primary: #6366F1 (indigo-500)
- Accent:  #F59E0B (amber-500)
- Tipografia: Inter (UI) + Poppins (display)
- Logo: pata estilizada em hexágono

Localização do projeto: ~/projects/pet-hub/
GitHub: github.com/alispnor/pet-hub
Branch principal: master

Fase atual: [SUBSTITUIR PELO NÚMERO DA FASE ANTES DE COLAR]
</projeto_atual>

<instrucoes_gerais>
1. SEMPRE leia a skill relevante antes de gerar código frontend
2. SEMPRE use context7 MCP para confirmar APIs de libs antes de usar
3. SEMPRE siga o padrão de template de função do Ali em todos os métodos
4. NUNCA gere UI genérica — aplique as skills de design para qualidade premium
5. NUNCA commite .env com valores reais — só .env.example
6. NUNCA armazene senhas, tokens ou dados sensíveis em logs
7. SEMPRE use Conventional Commits
8. SEMPRE prefira código reutilizável e padronizado
9. Quando gerar componentes Angular, use Standalone Components (sem NgModules)
10. Quando tiver dúvida sobre uma API, consulte context7 antes de assumir
</instrucoes_gerais>
```

---

## 📋 Como usar na prática

### Cada vez que abrir o Claude Code no projeto:

```bash
cd ~/projects/pet-hub
claude
```

**Passo 1:** Cola o bloco de ativação acima (este arquivo)
**Passo 2:** Substitui `[SUBSTITUIR PELO NÚMERO DA FASE]` pelo número correto (ex: `Fase atual: 1`)
**Passo 3:** Cola o prompt da fase correspondente logo em seguida

### Exemplo completo de sessão:

```
[Cola o bloco de ativação com "Fase atual: 1"]

[Cola o PROMPT-INICIAL-PET-HUB.md]  ← ver prompt-execucao-fase-0-1.md
```

O Claude Code vai:
1. Ler as skills de frontend antes de gerar qualquer tela Angular
2. Consultar context7 para confirmar APIs atualizadas
3. Aplicar os padrões que Ali já usa nos projetos anteriores
4. Manter consistência visual com a identidade do Pet Hub

---

## 🔄 Para fases com Angular (Fases 5 e 6)

Nas fases de frontend, adicione esta linha extra logo após colar o bloco de ativação:

```
Antes de gerar qualquer componente Angular, leia as skills:
- ~/.claude/skills/frontend-design/SKILL.md
- ~/.claude/skills/high-end-visual-design/SKILL.md
- ~/.claude/skills/web-design-guidelines/SKILL.md

Aplique o que aprender nessas skills em TODOS os componentes gerados.
O resultado deve ter qualidade visual de produto real de mercado,
não de tutorial ou boilerplate genérico.
Use a paleta do Pet Hub: indigo #6366F1 como primary, amber #F59E0B como accent.
```

---

## 💡 Por que isso funciona

O Claude Code tem acesso ao sistema de arquivos da sua máquina. Ao mencionar
explicitamente as skills e plugins:

1. **Skills** — ele vai ler os SKILL.md e aplicar as diretrizes de design e código
2. **context7** — ele vai consultar documentação atualizada das libs via MCP, evitando usar APIs depreciadas
3. **Projetos anteriores** — ele entende os padrões que você já usa e mantém consistência
4. **Identidade visual** — toda tela gerada vai usar as cores e tipografia do Pet Hub

O resultado é código mais próximo do que você já faz nos seus projetos reais,
com UI de qualidade e APIs corretas e atualizadas.

---

## ⚠️ Observação para futuras revisões

- Branch principal declarada aqui como `master`, mas o repo local está em `main` (verificar em `git status` antes de comandos que dependem do nome da branch).
