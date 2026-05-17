# Storefront Slice 4 — Minha conta + Timeline visual

> **Status:** Draft · 2026-05-17
> **Owner:** Ali
> **Phase:** 5 (Frontend Storefront) — Slice 4 de 4 (último)
> **Predecessors:** Slices 1-3 (`origin/main`)
> **Successor:** Fase 6 (Frontend Admin)

## 1. Objetivo

Fechar a Fase 5 entregando a área autenticada do storefront:

- `/minha-conta` overview com cards de dados ao vivo (perfil, endereços, cartões, pets, pedidos recentes).
- CRUDs completos em `modules/customer/pages/{perfil,enderecos,cartoes,pets}/`.
- Módulo novo `modules/orders/` com listagem paginada e detalhe de pedido + **`OrderTimelineComponent`** (destaque visual da Fase 5).
- Habilitar o CTA "Acompanhar pedido" da tela de sucesso do slice 3 (hoje `disabled`).
- `shared/pipes/safe-html.pipe.ts` com DOMPurify e substituição do escape manual no `product-detail`.

Não-objetivos (fora deste slice):

- Ação "cancelar pedido" pelo cliente — exige regra de negócio (janela, reembolso) que não está pronta no backend.
- Mudar email/senha do perfil — exige fluxo dedicado (LGPD, verificação) fora do escopo.
- Testes formais Karma/Jest — continuam dívida acumulada (mesmo bloqueio das fases 1-4); fica registrado em `fase-5-pendencias.md` quando o slice fechar.
- AppSec pendências (JWT-1, CORS-1, VAL-1, LOG-2) — endereçar no caminho para staging, não neste slice.

## 2. Decisões tomadas (durante o brainstorming)

| Decisão | Escolha | Justificativa |
|---|---|---|
| Estilo da OrderTimeline | **Híbrido** — horizontal em `≥ md`, vertical em `< md` | Cobre desktop e mobile com a melhor leitura em cada viewport. Mais código, melhor UX. |
| Layout dos CRUDs | **Rotas separadas por entidade** | URLs share-áveis, back/forward natural, segue padrão do checkout (1 rota = 1 step). |
| Conteúdo dos cards do overview | **Dados ao vivo** (forkJoin) | UX melhor que contadores secos; falha em 1 card não bloqueia outros. |
| Padrão de edição nos CRUDs | **Lista + form inline expansível** | Sem modal aninhado, segue pattern do checkout slice 3 (cadastro inline). |
| Navegação dentro de /minha-conta | **Sidebar vertical em md+, drawer hambúrguer no mobile** | Padrão de back-office moderno; permanece visível durante navegação entre sub-rotas. |
| Edição de cartão | **Sem edição — só add/remove/tornar padrão** | PCI: mudar PAN exige novo cadastro/tokenização. |
| Cancelar pedido | **Não implementado neste slice** | Regra de negócio (janela 24h, reembolso) não está pronta no backend. |
| Mini-timeline na lista de pedidos | **NÃO** | Evita 1 chamada extra por linha. Só badge na lista; timeline full no detalhe e compact no overview/sucesso. |
| Sanitização HTML | **DOMPurify** com whitelist conservadora | Substitui escape manual no `product-detail`; suporta tags básicas (`<p>`, `<strong>`, `<ul>`, `<a>`). |
| Confirm dialog | **`<dialog>` nativo HTML5** | Focus trap, ESC, backdrop click nativos. Sem lib externa. |
| Toast | **Service próprio com signal de fila** | Sem lib externa; consistente com decisão "no third-party UI lib" do projeto. |
| Skeleton vs spinner | **Skeleton** (blocos pulsantes) | UX melhor durante load inicial; extraído do `product-detail` para `shared/components/skeleton/`. |

## 3. Arquitetura

### 3.1 Estrutura de arquivos

```
src/app/
├── modules/
│   ├── customer/
│   │   ├── models/
│   │   │   ├── address.ts                  (existe)
│   │   │   ├── payment-method.ts           (existe)
│   │   │   ├── perfil.ts                   ⊕ novo
│   │   │   └── pet.ts                      ⊕ novo
│   │   ├── services/
│   │   │   ├── address.service.ts          (existe)
│   │   │   ├── payment-method.service.ts   (existe)
│   │   │   ├── perfil.service.ts           ⊕ novo
│   │   │   └── pet.service.ts              ⊕ novo
│   │   └── pages/
│   │       ├── account-shell/   ⊕ layout com sidebar + <router-outlet>
│   │       ├── overview/        ⊕ /minha-conta
│   │       ├── perfil/          ⊕ /minha-conta/perfil
│   │       ├── enderecos/       ⊕ /minha-conta/enderecos
│   │       ├── cartoes/         ⊕ /minha-conta/cartoes
│   │       └── pets/            ⊕ /minha-conta/pets
│   └── orders/                  ⊕ módulo novo
│       ├── models/order.ts
│       ├── services/order.service.ts
│       ├── components/
│       │   └── order-timeline/  ⊕ destaque visual da Fase 5
│       └── pages/
│           ├── lista/           ⊕ /minha-conta/pedidos
│           └── detalhe/         ⊕ /minha-conta/pedidos/:numero
└── shared/
    ├── pipes/
    │   └── safe-html.pipe.ts    ⊕ DOMPurify
    ├── components/
    │   ├── skeleton/            ⊕ extraído do product-detail
    │   ├── confirm-dialog/      ⊕ <dialog> nativo
    │   └── toast-stack/         ⊕ render da fila do ToastService
    └── services/
        └── toast.service.ts     ⊕ signal de fila + auto-dismiss
```

### 3.2 Routing (`app.routes.ts`)

```ts
{
  path: 'minha-conta',
  canActivate: [authGuard],
  loadComponent: () => import('@modules/customer/pages/account-shell/account-shell.page')
    .then(m => m.AccountShellPage),
  children: [
    { path: '',           loadComponent: () => import('@modules/customer/pages/overview/overview.page').then(m => m.OverviewPage) },
    { path: 'perfil',     loadComponent: () => import('@modules/customer/pages/perfil/perfil.page').then(m => m.PerfilPage) },
    { path: 'enderecos',  loadComponent: () => import('@modules/customer/pages/enderecos/enderecos.page').then(m => m.EnderecosPage) },
    { path: 'cartoes',    loadComponent: () => import('@modules/customer/pages/cartoes/cartoes.page').then(m => m.CartoesPage) },
    { path: 'pets',       loadComponent: () => import('@modules/customer/pages/pets/pets.page').then(m => m.PetsPage) },
    { path: 'pedidos',    loadComponent: () => import('@modules/orders/pages/lista/lista.page').then(m => m.OrdersListaPage) },
    { path: 'pedidos/:numero', loadComponent: () => import('@modules/orders/pages/detalhe/detalhe.page').then(m => m.OrdersDetalhePage) },
  ],
},
```

`authGuard` aplicado uma vez no shell — sub-rotas herdam.

### 3.3 Services novos

| Service | Estado | Responsabilidade |
|---|---|---|
| `PerfilService` | (stateless) | `GET /api/v1/customers/me`, `PUT /api/v1/customers/me`. |
| `PetService` | (stateless) | `GET/POST/PUT/DELETE /api/v1/customers/me/pets` + `POST /api/v1/customers/me/pets/{id}/foto` (multipart). |
| `OrderService` | (stateless) | `GET /api/v1/orders` (paginado), `GET /api/v1/orders/{numero}`, `GET /api/v1/orders/{numero}/timeline`. |
| `ToastService` | `mensagens = signal<Toast[]>` | `success(msg)`, `error(msg)`, `info(msg)`, auto-dismiss 5s, dedup por timestamp. |

Services existentes (`AddressService`, `PaymentMethodService`, `AuthService`, `CartService`) são consumidos sem alteração.

### 3.4 Componentes novos

| Componente | Standalone | Inputs | Notas |
|---|---|---|---|
| `AccountShellPage` | ✅ | — | Sidebar vertical em md+, drawer no mobile via signal `sidebarAberta`. `<router-outlet>` no main. Logout no rodapé do sidebar. |
| `OrderTimelineComponent` | ✅ | `timeline: Timeline` (required), `compact = false` | Híbrido h-md+/v-mobile. `aria-current="step"` na ATUAL. `prefers-reduced-motion` desabilita pulse. |
| `SkeletonComponent` | ✅ | `lines = 3`, `variant: 'text' \| 'card' \| 'avatar'` | Extraído do product-detail. Tailwind `animate-pulse`. |
| `ConfirmDialogComponent` | ✅ | (API via service: `ConfirmDialogService.open({...})`) | `<dialog>` nativo. Retorna `Promise<boolean>`. |
| `ToastStackComponent` | ✅ | — | Lê `ToastService.mensagens`. Fixed top-right. Animação fade-in/slide. |

## 4. Páginas — comportamento por tela

### 4.1 `AccountShellPage` (`/minha-conta`)

Layout:
- **md+**: grid 2 colunas (sidebar 240px fixa + main flex).
- **< md**: main full-width; sidebar vira drawer slide-in da esquerda, abre via botão hambúrguer no header global (`MainLayoutComponent` ganha esse botão visível só nas rotas `/minha-conta/**`).

Sidebar items:
```
○ Visão geral       /minha-conta
○ Perfil            /minha-conta/perfil
○ Endereços         /minha-conta/enderecos
○ Cartões           /minha-conta/cartoes
○ Pets              /minha-conta/pets
○ Pedidos           /minha-conta/pedidos
─────────────────
[Sair]              (logout)
```

Item ativo destacado via `routerLinkActive="..."`. ESC fecha drawer no mobile.

### 4.2 `OverviewPage` (`/minha-conta`)

5 chamadas paralelas no `ngOnInit`:

```ts
forkJoin({
  perfil:     this.perfilService.me().pipe(catchError(() => of({ erro: true }))),
  enderecos:  this.addressService.listar().pipe(catchError(() => of({ erro: true }))),
  cartoes:    this.paymentMethodService.listar().pipe(catchError(() => of({ erro: true }))),
  pets:       this.petService.listar().pipe(catchError(() => of({ erro: true }))),
  pedidos:    this.orderService.listar({ page: 0, size: 3 }).pipe(catchError(() => of({ erro: true }))),
})
```

Cada card consome um signal próprio derivado da response. Erro isolado por card (mostra "Não foi possível carregar" + retry).

Layout dos cards (grid responsivo `grid-cols-1 md:grid-cols-2 lg:grid-cols-3`):

1. **Perfil** — avatar com iniciais, nome, CTA "Editar perfil".
2. **Endereços** — qtd cadastrados, endereço padrão de entrega resumido, CTA "Gerenciar".
3. **Cartões** — qtd cadastrados, último cartão (bandeira + last4), CTA "Gerenciar".
4. **Pets** — qtd cadastrados, 1º pet (nome + espécie), CTA "Gerenciar".
5. **Pedidos recentes** (col-span 2) — 3 últimos com badge de status, valor, número; pedido ATIVO (não-terminal) mostra `OrderTimelineComponent` em modo compact.

### 4.3 `PerfilPage` (`/minha-conta/perfil`)

Form único com Reactive Forms:

| Campo | Editável | Validação | Notas |
|---|---|---|---|
| Nome | ✅ | required, min 2 | |
| Email | ❌ read-only | — | Mudar exige fluxo dedicado (não neste slice). |
| CPF | ❌ read-only | — | Mascarado pelo backend (`***.***.***-12`). |
| Telefone | ✅ | regex BR | Máscara `(11) 91234-5678` via diretiva existente. |
| Data nasc. | ✅ | date, idade ≥ 18 | `<input type="date">`. |
| Gênero | ✅ | — | `<select>` (M, F, NB, Prefiro não informar). |

CTA "Salvar alterações" ativa quando `form.dirty`. Toast de sucesso. Erro 422 mapeia `ProblemDetail.errors[]` pros campos.

### 4.4 `EnderecosPage` (`/minha-conta/enderecos`)

Lista de cards. Cada card mostra:
- Apelido (Casa, Trabalho, etc.) + badges "★ Padrão entrega" / "★ Padrão cobrança".
- Endereço completo (logradouro, número, complemento, bairro, cidade/UF, CEP).
- Ações: `[Editar]` `[Tornar padrão …]` `[Remover ✕]`.

Comportamento:
- Botão "Adicionar endereço" no topo → expande form vazio.
- "Editar" → expande form no próprio card e collapse os demais.
- "Remover" → `ConfirmDialogService.open({ titulo: 'Remover endereço?', acaoVariant: 'danger' })`.
- Form com ViaCEP autofill **reusando lógica do `checkout/pages/address`** — extrair pra um helper compartilhado em `modules/customer/services/cep-autofill.ts` ou similar (decidir durante implementação; sem criar abstração prematura).
- Checkboxes "Definir como padrão de entrega" / "...de cobrança".

### 4.5 `CartoesPage` (`/minha-conta/cartoes`)

Lista de cards. Cada card mostra:
- Bandeira (Visa/Master/...), `•••• {last4}`, validade, titular.
- Badge "★ Padrão".
- Ações: `[Tornar padrão]` `[Remover ✕]` (sem "Editar").

Adicionar cartão (form inline expansível):
1. Validação Luhn client-side.
2. `POST /api/v1/customers/payment-methods/tokenize` → recebe token.
3. **Limpa PAN/CVV do form imediatamente** após sucesso da tokenize (`formularioCartao.patchValue({ numero: '', cvv: '' })` dentro do `next:` ANTES do `create()` chained — mesma decisão do slice 3).
4. `POST /api/v1/customers/me/payment-methods` com token.
5. CVV `type="password"` cru, sem toggle olho.

### 4.6 `PetsPage` (`/minha-conta/pets`)

Lista de cards. Cada card mostra:
- Foto (ou placeholder) + nome do pet.
- Espécie, raça, idade (calculada de dataNasc), peso, sexo.
- Ações: `[Editar]` `[Remover ✕]`.

Form inline com:
- nome (required, min 2)
- espécie (required, select: cachorro/gato/outro)
- raça (text, opcional)
- dataNascimento (date, opcional)
- peso (number, opcional)
- sexo (select: macho/fêmea/não informado)
- observações (textarea, opcional)
- Upload de foto:
  - `<input type="file" accept="image/*">` com preview client-side via `URL.createObjectURL()`.
  - POST `/api/v1/customers/me/pets/{id}/foto` (multipart) — submetido após o save do pet (precisa do ID).
  - Limite client-side: 5MB (validação antes do upload; backend valida também).

### 4.7 `OrdersListaPage` (`/minha-conta/pedidos`)

Filtros sincronizados com URL (`?page=0&size=10&status=SEPARACAO&dataInicio=...&dataFim=...`):

- `<select>` Status (todos os enum `StatusPedido` + opção "Todos")
- `<input type="date">` De
- `<input type="date">` Até
- Botão `[Limpar filtros]`

Mudanças nos filtros: navegação programática `router.navigate([], { queryParams: ..., queryParamsHandling: 'merge' })`. Date inputs com debounce 250ms via `rxjs/debounceTime`.

Lista renderiza `Page<PedidoResumoResponse>` do backend. Cada item:
- Número, valor total, badge de status com cor.
- Data, qtd itens, forma de pagamento (tipo).
- CTA "Ver detalhe →" link pra `/minha-conta/pedidos/:numero`.
- **Sem mini-timeline** na lista (decisão custo de payload).

Paginação no rodapé: `« Anterior | Página X de Y | Próximo »`.

### 4.8 `OrdersDetalhePage` (`/minha-conta/pedidos/:numero`)

2 chamadas paralelas:

```ts
forkJoin({
  pedido:   this.orderService.detalhe(numero),
  timeline: this.orderService.timeline(numero),
})
```

Layout:
- Header: número, badge de status, data, link "← Voltar para Meus pedidos".
- **`OrderTimelineComponent` full** (não compact) — destaque visual.
- Grid 2 colunas: Itens (esquerda) | Resumo financeiro + forma pgto (direita).
- Grid 2 colunas: Endereço entrega | Endereço cobrança.

**Snapshots tipados:** `PedidoResponse.enderecoEntrega` vem como `Map<String, Object>`. Criar tipo `EnderecoSnapshot` no frontend:

```ts
interface EnderecoSnapshot {
  apelido?: string;
  logradouro: string;
  numero: string;
  complemento?: string;
  bairro: string;
  cidade: string;
  uf: string;
  cep: string;
}
```

Cast tipado no service: `pedido.enderecoEntrega as EnderecoSnapshot`. Mesmo para `opcaoFrete`.

Itens com snapshot (não consultar `/catalog/products` — sempre mostra o que foi cobrado): `PedidoItemResponse.skuProduto`, `nomeProduto`, `precoUnitario`, `quantidade`, `fotoUrl`.

Estados terminais:
- `ENTREGUE` → timeline todas concluídas, badge verde.
- `CANCELADO` → timeline corta + etapa "Cancelado" em vermelho (backend já manda).
- `PAGAMENTO_REJEITADO` → mesmo tratamento.
- `DEVOLVIDO` → etapa extra após "Entregue".

## 5. OrderTimelineComponent — detalhes

### 5.1 Contrato

```ts
@Component({
  selector: 'app-order-timeline',
  standalone: true,
  templateUrl: './order-timeline.component.html',
  styleUrls: ['./order-timeline.component.scss'],
})
export class OrderTimelineComponent {
  @Input({ required: true }) timeline!: Timeline;
  @Input() compact = false;
}
```

Tipo `Timeline` no frontend espelha `TimelineResponse` do backend:

```ts
export type StatusEtapa = 'CONCLUIDA' | 'ATUAL' | 'PENDENTE';

export interface EtapaTimeline {
  nome: string;
  status: StatusEtapa;
  ocorridoEm: string | null;       // ISO datetime
  tempoDecorrido: string | null;   // "há 3 horas"
  previsaoEntrega: string | null;  // ISO date ou texto
}

export interface Timeline {
  numeroPedido: string;
  statusAtual: StatusPedido;
  etapas: EtapaTimeline[];
}
```

### 5.2 Layout responsivo

- **`≥ md`** (horizontal): `<ol>` com `flex-row`, conector entre `<li>` via `::after` pseudo-element. Cada `<li>` contém o ícone/bolinha no topo, label e meta dados embaixo (centralizados).
- **`< md`** (vertical): mesmo `<ol>` com `flex-col`, rail visual à esquerda via `border-left` no `<li>` (exceto último), bolinhas absolutamente posicionadas sobre o rail.

CSS via Tailwind utility + alguns custom rules em `.scss` pra pseudo-elements (não dá pra fazer puramente com utilities).

### 5.3 Estados visuais

| Status | Bolinha | Conector | Label | Acessibilidade |
|---|---|---|---|---|
| `CONCLUIDA` | preenchida `primary-600` + ícone check | sólido `primary-600` | regular | — |
| `ATUAL` | preenchida + `ring-4 ring-primary-200 animate-pulse` | sólido | **bold** | `aria-current="step"` |
| `PENDENTE` | vazia, `border-2 border-gray-300` | tracejado `border-dashed gray-300` | `text-gray-500` | — |

Etapa de erro (status `CANCELADO`, `PAGAMENTO_REJEITADO`): override pra `red-500` na bolinha + label. Etapa `DEVOLVIDO` (estado pós-entrega legítimo, não-erro): override pra `amber-500` (sinalização diferenciada do erro).

### 5.4 Modo compact

`compact === true`:
- Sempre vertical (mesmo em md+).
- Bolinhas menores (`w-3 h-3` vs `w-5 h-5`).
- Sem `tempoDecorrido` / `previsaoEntrega` (só label).
- Altura total contida (max ~120px) pra caber no card de overview.

### 5.5 Acessibilidade

- `<ol>` semântico, `<li>` por etapa.
- `aria-current="step"` na etapa ATUAL.
- Ícones com `aria-hidden="true"` (texto acompanha sempre).
- `@media (prefers-reduced-motion: reduce)` desabilita `animate-pulse`.
- Cores nunca são o único sinal (status é texto + ícone também).

## 6. SafeHtmlPipe

`shared/pipes/safe-html.pipe.ts`:

```ts
@Pipe({ name: 'safeHtml', standalone: true })
export class SafeHtmlPipe implements PipeTransform {
  private readonly sanitizer = inject(DomSanitizer);

  transform(value: string | null | undefined): SafeHtml {
    if (!value) {
      return '';
    }
    const limpo = DOMPurify.sanitize(value, {
      ALLOWED_TAGS: ['p', 'br', 'strong', 'em', 'ul', 'ol', 'li', 'a'],
      ALLOWED_ATTR: ['href', 'target', 'rel'],
      ALLOW_DATA_ATTR: false,
    });
    return this.sanitizer.bypassSecurityTrustHtml(limpo);
  }
}
```

Dependência nova: `npm i dompurify @types/dompurify`.

**Substituição no `product-detail.page`:**
- Antes: `[innerHTML]="descricaoSanitizada(produtoExibido.descricaoCompleta)"` (função no `.ts` que escapa manualmente; comentário declara "Quando entrar um sanitizador real (Slice 4+), trocar...").
- Depois: `[innerHTML]="produtoExibido.descricaoCompleta | safeHtml"`.
- Remover a função `descricaoSanitizada()` do `.ts` e o comentário associado.

Tags permitidas conservadoras: sem `<img>`, `<script>`, `<iframe>`, sem inline styles. Ampliar só sob demanda real.

## 7. Patterns transversais

### 7.1 Loading
- `SkeletonComponent` em `shared/components/skeleton/` com variantes `text`, `card`, `avatar`.
- Botões em submitting: `disabled` + label `"Salvando..."` + `<svg>` spinner inline (pattern já em `checkout/review`).

### 7.2 Erro

| Cenário | Apresentação |
|---|---|
| Falha no resolver/load inicial (página inteira) | Banner de erro no topo + botão `[Tentar novamente]` |
| Falha em 1 card do overview | Card específico com erro + retry; outros mantêm dados |
| Falha em mutation (delete/save) | Toast vermelho + form mantém estado |
| 401 (token inválido) | Já tratado em `authInterceptor` — redirect `/login` |
| 422 (validação backend) | Mensagens do `ProblemDetail.errors[]` mapeadas pros campos via `form.get(campo).setErrors({ backend: msg })` |
| 5xx | Toast genérico "Algo deu errado, tente novamente" + `console.error` |

### 7.3 ToastService

```ts
@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly mensagens = signal<Toast[]>([]);

  success(mensagem: string) { this.empilhar({ tipo: 'success', mensagem }); }
  error(mensagem: string)   { this.empilhar({ tipo: 'error', mensagem }); }
  info(mensagem: string)    { this.empilhar({ tipo: 'info', mensagem }); }

  private empilhar(toast: Omit<Toast, 'id'>) {
    const id = crypto.randomUUID();
    this.mensagens.update(lista => [...lista, { id, ...toast }]);
    setTimeout(() => this.dismiss(id), 5000);
  }

  dismiss(id: string) {
    this.mensagens.update(lista => lista.filter(toast => toast.id !== id));
  }
}
```

`ToastStackComponent` renderizado uma vez em `MainLayoutComponent`, `fixed top-4 right-4 z-50`.

### 7.4 ConfirmDialog

`shared/components/confirm-dialog/confirm-dialog.component.ts` + `confirm-dialog.service.ts`:

```ts
@Injectable({ providedIn: 'root' })
export class ConfirmDialogService {
  open(opcoes: {
    titulo: string;
    mensagem: string;
    acaoLabel: string;
    acaoVariant?: 'primary' | 'danger';
  }): Promise<boolean> { /* ... */ }
}
```

Implementação: `<dialog>` nativo HTML5, `showModal()`. Retorna `Promise<boolean>` resolvida quando usuário confirma/cancela ou fecha (ESC, backdrop). Focus trap nativo. Sem lib externa.

### 7.5 Habilitação do CTA "Acompanhar pedido"

No `checkout/pages/success/success.page`:
- Hoje: `<button disabled title="Disponível em breve">Acompanhar pedido</button>`.
- Depois: `<a [routerLink]="['/minha-conta/pedidos', numero]" class="...">Acompanhar pedido</a>`.

## 8. Sequência de implementação sugerida

1. **Fundação shared** — SafeHtmlPipe, ToastService + ToastStack, ConfirmDialog, Skeleton. (Atomic, não toca user flow.)
2. **Substituir escape manual no product-detail** (1 commit pequeno, testa pipe).
3. **AccountShellPage + routing** — sidebar, drawer mobile, hambúrguer no header global, sub-rotas vazias.
4. **OverviewPage** — forkJoin, cards, retry por card.
5. **PerfilPage** — único form, validações.
6. **EnderecosPage** — lista + form inline + ViaCEP + confirm dialog.
7. **CartoesPage** — lista + form inline com tokenização (reusa lógica do checkout slice 3).
8. **PetsPage** — lista + form inline + upload de foto.
9. **OrdersListaPage** — paginação + filtros sync URL.
10. **OrderTimelineComponent + OrdersDetalhePage** — destaque visual + snapshots.
11. **Habilitar CTA na success page do checkout**.
12. **Smoke E2E manual** — fluxo completo logado.

## 9. Smoke checklist (validação manual no browser ao fim)

- [ ] `/minha-conta` carrega 5 cards, falha em 1 não bloqueia outros.
- [ ] Sidebar destaca item ativo, hambúrguer abre drawer no mobile, ESC fecha.
- [ ] Logout no sidebar funciona.
- [ ] `/perfil` salva alterações, mostra toast de sucesso, dirty desativa botão se reverter.
- [ ] `/enderecos`: add/edit/delete com confirm dialog, ViaCEP autofill, marcar padrão.
- [ ] `/cartoes`: add com tokenize (PAN/CVV limpos), marcar padrão, delete.
- [ ] `/pets`: add/edit/delete com upload de foto, preview client-side antes do submit.
- [ ] `/pedidos`: paginação, filtros sync URL, badge por status correto.
- [ ] `/pedidos/:numero`: timeline full renderiza, snapshots dos itens corretos, estados terminais (cancelado, entregue) corretos.
- [ ] CTA "Acompanhar pedido" da tela de sucesso navega pro detalhe.
- [ ] product-detail renderiza descrição com `<strong>`/`<p>` (não escape literal); `<script>` injetado é stripado.
- [ ] `prefers-reduced-motion`: pulse desabilitado.
- [ ] Teclado: tab navega cards/forms, Enter submete, ESC fecha drawer/dialog.

## 10. Dívida técnica e pendências pós-slice

### 10.1 Testes formais (mesmo bloqueio fases 1-4)

| Componente/Service | Testes mínimos |
|---|---|
| `OrderTimelineComponent` | renderiza N etapas, ATUAL com pulse, terminais sem futuras, compact funciona |
| `SafeHtmlPipe` | sanitiza `<script>`, mantém `<p>`/`<strong>`, lida com null/undefined |
| `AccountShellPage` | sidebar destaca ativo, hambúrguer abre drawer, ESC fecha |
| `OverviewPage` | falha em 1 card não bloqueia outros, retry funciona |
| `EnderecosPage` | inline form, ViaCEP autofill, delete com confirm |
| `CartoesPage` | tokenize wired, PAN/CVV limpos após sucesso |
| `PetsPage` | upload de foto, preview, validação de tamanho |
| `OrdersListaPage` | paginação sync URL, filtros sync, badge correto por status |
| `OrdersDetalhePage` | snapshot itens, timeline full, estados terminais |
| `ToastService` | empilha/dismiss, auto-dismiss 5s, dedup |
| `ConfirmDialogService` | Promise<boolean>, ESC/backdrop fecham com false |
| Cypress E2E | smoke: login → /minha-conta → /pedidos → detalhe com timeline |

### 10.2 Itens já fora do escopo (registrar em `fase-5-pendencias.md` ou ROADMAP)

- Cancelar pedido pelo cliente.
- Mudar email/senha do perfil (fluxo dedicado, LGPD).
- Reordenar/favoritar endereços.
- Histórico de cartões removidos.
- Notificações em tempo real de mudança de status (WebSocket/SSE — Fase 6+ admin).

### 10.3 AppSec

- `LOG-2` (mascaramento de PII em logs) — endereçar na transição pra Fase 6 conforme `appsec-pendencias.md`.
- Demais (JWT-1, CORS-1, VAL-1) — bloqueantes só no caminho pra staging.

## 11. Critérios de "pronto"

- Todas as 7 rotas novas (`/minha-conta`, `/perfil`, `/enderecos`, `/cartoes`, `/pets`, `/pedidos`, `/pedidos/:numero`) acessíveis e funcionais.
- Smoke checklist §9 verde no browser.
- `npm run build` sem erros nem warnings novos.
- CTA "Acompanhar pedido" habilitado e navega corretamente.
- `product-detail` usa `safeHtml` (escape manual removido).
- Commits Conventional Commits (`feat(storefront):` ou `feat(customer):`) com mensagens descritivas; um commit por marco da §8.
- Atualizar `ai-memory/roadmap/fase-5-pendencias.md`: marcar slice 4 como entregue, registrar testes formais como dívida.
- Atualizar `ROADMAP.md`: trocar status da Fase 5 de 🚧 para ✅.

## 12. Premissas técnicas

- Backend rodando em `http://localhost:8080` com Postgres em `localhost:5433` e Redis em `localhost:6380` (configuração atual do `.env.local`).
- Storefront em `http://127.0.0.1:4242` com proxy `/api/v1/* → :8080` (já configurado em `proxy.conf.json`).
- Usuário de teste: matriz no README (admin/gerente/operador/3 clientes — seed V16).
- Cartões de teste: `4111 1111 1111 1111` aprova; `4111 1111 1111 9400` rejeita (Luhn-válido, last4=4000).
- Browser-alvo: Chrome/Firefox/Safari atuais (sem suporte IE).

## 🛡️ OWASP & Security Checkpoint

| Vulnerabilidade OWASP | Mitigação aplicada neste slice |
|---|---|
| **A03:2021 — Injection / XSS** | `SafeHtmlPipe` com DOMPurify (whitelist conservadora) substitui escape manual no `product-detail`. Sem `[innerHTML]` cru em nenhum outro ponto. |
| **A01:2021 — Broken Access Control (BOLA)** | Backend já enforce ownership em `/me/*` (Fases 1-4). Frontend não envia IDs de outros usuários; mesmo se enviar, backend retorna 403. |
| **A02:2021 — Cryptographic Failures (PCI)** | Cartão: tokenize antes de salvar; PAN/CVV limpos do form imediatamente após sucesso da tokenize. Sem PAN/CVV em sessionStorage/localStorage. CVV `type="password"` cru. |
| **A04:2021 — Insecure Design** | Confirm dialog em todas as ações destrutivas (delete). Sem operação irreversível com 1 clique. |
| **A05:2021 — Security Misconfiguration** | CORS já configurado pra `http://127.0.0.1:4242` no backend (slice 1 hotfix). Headers de segurança ficam pra nginx em produção. |
| **A07:2021 — Authentication Failures** | `authGuard` no shell. Sub-rotas herdam. Logout limpa estado local + invalida refresh no backend (`POST /auth/logout` existente). |
| **A08:2021 — Software & Data Integrity Failures** | DOMPurify versão fixada em `package.json`. Sem inline scripts gerados dinamicamente. |
| **A09:2021 — Logging Failures** | Frontend não loga PII em `console.log` em produção (`environment.production` gate em logs existentes). Mutations passam pelo backend com auditoria. |
| **API1:2023 — BOLA** | Endpoints `/me/*` validam dono no backend; frontend só consome o que pertence ao current user. |
| **API3:2023 — BOPLA (excessive data exposure)** | DTOs do backend já mascaram (CPF parcial, last4 cartão); frontend exibe o que vier. Não tenta enriquecer com dados sensíveis. |

**Premissas de infra:**
- HTTPS obrigatório em staging/prod (atualmente HTTP em dev local — aceitável).
- `Content-Security-Policy` servido pelo nginx em prod (não vem do Angular).
- Cookies HttpOnly do refresh token já configurados (slice 1).
- LGPD: PII mostrada na UI (CPF mascarado, endereço completo do dono) — não exporta nem compartilha.
