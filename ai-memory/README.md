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
