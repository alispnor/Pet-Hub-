# Contributing

> Pet Hub é um projeto solo do Ali, mas escrito como se fosse colaborativo — disciplina de PR, commit history limpo e revisão por par mesmo quando "o par" é o Ali de daqui a 6 meses.

## Conventional Commits

Todos os commits seguem o padrão [Conventional Commits](https://www.conventionalcommits.org/).

### Formato

```
<tipo>(<scope opcional>): <descrição curta no imperativo>

<corpo opcional explicando o porquê>

<rodapé opcional: Co-Authored-By, refs a issue>
```

### Tipos aceitos

| Tipo | Uso |
|---|---|
| `feat` | Nova funcionalidade visível para o usuário |
| `fix` | Correção de bug |
| `refactor` | Mudança de código que não altera comportamento externo |
| `perf` | Otimização de performance |
| `test` | Adição ou ajuste de testes |
| `docs` | Documentação (README, ROADMAP, ADRs, comentários) |
| `chore` | Configurações, dependências, build scripts |
| `db` | Migrations Flyway, mudanças de schema |
| `ci` | GitHub Actions, pipelines de CI/CD |
| `style` | Formatação, ponto-vírgula, espaços — sem mudança de lógica |
| `build` | Maven/npm/Gradle config |

### Scopes

Use o nome do módulo backend ou app frontend como scope:

- `feat(catalog): add product search by category slug`
- `feat(identity): implement refresh token rotation`
- `fix(checkout): correct tax calculation for interstate orders`
- `feat(storefront): add timeline component to order detail`
- `test(inventory): cover concurrent reservation scenarios`
- `chore(backend): bump Spring Boot to 3.3.5`

### Boas práticas

- ✅ Descrição no imperativo: "add", não "added" ou "adds"
- ✅ Minúscula no início da descrição
- ✅ Sem ponto final na descrição curta
- ✅ Linha curta (≤72 caracteres). Detalhes no corpo.
- ✅ Corpo explica **por quê**, não **o quê** (o diff mostra o quê).
- ❌ Evitar "fixes", "updates", "changes" — muito genérico. Especifique o que mudou.

### Exemplos completos

```
feat(catalog): add NCM validation on product creation

NCM é obrigatório para emissão de NF-e (Fase 7). Validação rejeita
formatos inválidos cedo (no controller) em vez de só na emissão.
```

```
fix(checkout): preserve cart when payment fails

Anteriormente, falha de pagamento limpava o carrinho — usuário tinha
que adicionar tudo de novo. Agora o carrinho é mantido e o usuário
pode retentar com outra forma de pagamento.

Refs: #42
```

## Branch strategy

### Branches permanentes

- `main` — branch principal, sempre em estado deployável.

### Branches de trabalho

- `feature/<nome-curto>` — nova funcionalidade. Ex.: `feature/checkout-pix`.
- `fix/<nome-curto>` — correção de bug. Ex.: `fix/nfe-cfop-interstate`.
- `hotfix/<nome-curto>` — correção urgente em produção. Ex.: `hotfix/payment-timeout`.
- `refactor/<nome-curto>` — refactor sem mudança de comportamento.

### Fluxo

1. Crie branch a partir de `main`.
2. Faça commits granulares com Conventional Commits.
3. Push da branch e abra Pull Request contra `main`.
4. PR só é mergeado depois de CI passar (Fase 12 em diante; até lá CI é placeholder).
5. Merge usando **squash & merge** ou **rebase** para manter `main` linear.
6. Delete a branch após o merge.

### Exceções

- Fase 0 (Bootstrap): commits diretos em `main` são aceitáveis porque o repo ainda não tem o que proteger.
- Hotfixes críticos em produção: podem ir direto, com retroativo de revisão.

## Pull Request workflow

Use o template em `.github/PULL_REQUEST_TEMPLATE.md`. Em cada PR:

- **Descrição:** uma linha sobre o que muda + uma sobre o porquê.
- **Tipo de mudança:** feature, fix, refactor, docs, etc.
- **Como testar:** passos para validar a mudança localmente.
- **Checklist:**
  - [ ] Testes adicionados/atualizados quando aplicável
  - [ ] Docs atualizadas (README, ROADMAP, ADRs) quando aplicável
  - [ ] Lint passa
  - [ ] CI verde

PRs grandes (>500 LOC) são desencorajados — quebrar em PRs menores facilita revisão.

## Code style

Estilo padronizado via [`.editorconfig`](./.editorconfig):

- Java: 4 espaços, linha máxima 120 chars.
- JS/TS/HTML/CSS/SCSS/YAML/JSON/Markdown: 2 espaços.
- Makefiles: tab (mandatório).
- LF line endings, UTF-8, newline final.

### Java

- DTOs como records imutáveis.
- MapStruct para conversões (nunca conversão manual).
- Lombok `@Slf4j` em todas as classes com log.
- Exceptions de negócio estendem `BusinessException` (módulo `common`).
- Sem lógica de negócio em controllers.
- Template de função em todos os métodos públicos de service:

```java
public Type metodo(Params params) {
    log.debug("Iniciando {} com params={}", "metodo", params);
    validar(params);
    var resultado = executarLogica(params);
    log.debug("Finalizado {} com resultado={}", "metodo", resultado);
    return resultado;
}
```

### TypeScript / Angular

- `strict` mode ligado no `tsconfig.json`.
- Standalone Components (sem NgModules).
- Signals para estado local; Service com Signal para estado compartilhado.
- Sem `any`. Se precisar, use `unknown` + narrowing.

## Segurança

- ❌ NUNCA commitar `.env` com valores reais. Use `.env.example`.
- ❌ NUNCA commitar chaves, tokens, senhas, certificados.
- ❌ NUNCA logar senhas, tokens, CPF completo, números de cartão.
- ✅ Use `EncryptedStringConverter` para PII em repouso.
- ✅ BCrypt cost 12 para senhas.
- ✅ Rate limit em endpoints sensíveis (login, register).

## Idioma

- **Código, identificadores, nomes de arquivos, branches, commit messages:** inglês.
- **Documentação (README, ROADMAP, ADRs, comentários quando muito longos), templates de issue/PR:** português (PT-BR informal).

## Dúvidas

Como o projeto é solo, dúvidas vão direto para o `MEMORY.md` do Claude Code do Ali, ou em uma issue no repo. Para colaboradores externos eventuais: abra uma issue antes de começar um PR grande.
