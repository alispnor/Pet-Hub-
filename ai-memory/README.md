# ai-memory

Base de conhecimento persistente do projeto Pet Hub para uso por agentes de IA (Claude Code e outros).

Diferente do `CLAUDE.md` (que é um índice curto, sempre carregado no contexto), os arquivos aqui servem para guardar conteúdo mais longo: decisões arquiteturais, contexto de domínio, notas de reuniões, especificações, etc.

## Estrutura sugerida

```
ai-memory/
├── README.md           # este arquivo
├── decisions/          # ADRs — registros de decisões arquiteturais
├── domain/             # contexto de negócio, glossário, regras de domínio
├── architecture/       # diagramas, fluxos, contratos entre serviços
├── integrations/       # notas sobre Pet Diary, Claude API, gateways de pagamento
├── roadmap/            # detalhamento das 12 fases (quando existirem)
└── notes/              # rascunhos e observações soltas
```

Crie subpastas conforme necessário — não há obrigação de seguir essa estrutura à risca.

## Convenções

- Um assunto por arquivo. Nome do arquivo em `kebab-case.md`.
- Datar entradas de log/notas (`YYYY-MM-DD`) para facilitar revisão posterior.
- Marcar status quando aplicável: `[draft]`, `[approved]`, `[superseded]`.
- Arquivos aqui são versionados no git — não colocar segredos.

## Documentos fundacionais (leitura obrigatória ao desenvolver)

Estes documentos estabelecem padrões mandatórios. Toda geração/refatoração de código no projeto deve cumpri-los — não são opcionais.

| Arquivo | Escopo |
|---|---|
| [`architecture/appsec-guidelines.md`](architecture/appsec-guidelines.md) | **Security by Design.** Regras OWASP (Web Top 10 + API Top 10 + Mobile Top 10) aplicadas ao Pet Hub. Todo código gerado precisa terminar com bloco `🛡️ OWASP & Security Checkpoint`. |
| [`design-system.md`](design-system.md) | Design system v1 (brand, tokens, componentes, anti-patterns). Mandatório para qualquer trabalho de UI. |
| [`roadmap/plano-completo.md`](roadmap/plano-completo.md) | Plano estratégico v2 — 12 fases. Fonte de verdade para escopo. |
| [`roadmap/appsec-pendencias.md`](roadmap/appsec-pendencias.md) | Achados de AppSec em código já mergeado, com marco/prazo para resolução. |
