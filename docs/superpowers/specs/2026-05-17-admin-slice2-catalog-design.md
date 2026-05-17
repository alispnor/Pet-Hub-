# Admin Slice 6.2 — Catálogo (Produtos + Categorias + Vídeo)

> **Status:** Draft · 2026-05-17
> **Owner:** Ali
> **Phase:** 6 (Frontend Admin) — Slice 2 de 6
> **Predecessor:** Slice 6.1 ✅ entregue em 2026-05-17
> **Successor:** Slice 6.3 (Operações)
> **Decomposição completa da fase:** [`ai-memory/roadmap/fase-6-decomposicao.md`](../../../ai-memory/roadmap/fase-6-decomposicao.md)

## 1. Objetivo

Entregar CRUDs admin de Produtos e Categorias no `frontend/admin/`, incluindo upload de imagens com drag-and-drop nativo HTML5 e a **novidade da Fase 6**: vídeo de produto via URL externa (YouTube/Vimeo) com sandbox no `<iframe>` do storefront.

**Entregáveis concretos:**

Backend:
- Flyway V18: `ALTER TABLE produtos ADD COLUMN video_url VARCHAR(500)`.
- Entidade `Produto` ganha campo `videoUrl`.
- Validador custom `@ValidVideoEmbedUrl` (regex whitelist YouTube/Vimeo).
- `ProdutoCriarRequest`, `ProdutoAtualizarRequest`, `ProdutoResponse`, `ProdutoDetailResponse` atualizados.
- MapStruct mapper atualizado.

Frontend admin (`frontend/admin/`):
- Módulo novo `modules/catalog/` com models + services + páginas.
- `/admin/produtos` lista paginada custom (Tailwind, sem libs externas), filtros URL-sync.
- `/admin/produtos/novo` e `/admin/produtos/:sku/editar` — mesma `ProdutoFormPage` com modo via rota.
- `/admin/categorias` lista plana indentada + form inline (padrão `EnderecosPage`).
- Componentes: `ImageDropZoneComponent` (HTML5 drop nativo), `VideoUrlInputComponent` (input + preview iframe).
- Sidebar do `AdminShellPage` habilita item "Catálogo".

Frontend storefront:
- `SafeResourceUrlPipe` em `shared/pipes/` (bypass para iframe src).
- `product-detail.page` ganha seção condicional de vídeo com `<iframe sandbox>`.
- `urlEmbed()` helper converte URL pública → `/embed/` (YouTube/Vimeo).
- Modelo `ProdutoDetail` ganha `videoUrl: string | null`.

**Não-objetivos:**

- Bulk actions (selecionar múltiplos produtos + ação em massa) — fica pro slice 6.5 ou backlog.
- Reordenar imagens via DnD — **backend não suporta** (só POST/DELETE); ordem é FIFO via `criadoEm`. Endpoint de reordenar fica como dívida.
- Upload de vídeo interno (arquivo) — só URL externa nesta fase; armazenamento próprio é backlog (storage/CDN justifica revisão).
- Audit log das ações de catálogo — entra junto com Slice 6.3.
- Testes formais Karma/Jest — continua dívida acumulada.

## 2. Decisões tomadas (durante o brainstorming)

| Decisão | Escolha | Justificativa |
|---|---|---|
| Tabela de produtos | **Custom standalone (Tailwind)** | Consistente com storefront (sem libs UI externas). ~150 linhas, sort/filter client-side, paginação server-side. ag-Grid pesado demais (~600KB) pra ganho marginal. |
| Categorias UX | **Lista plana + indentação + `<select>` de pai no form** | Backend já é hierárquico (`categoriaPai` self-ref). UX simples sem libs de tree. Backend valida ciclo. |
| Upload de imagens | **HTML5 drop nativo + multipart sequencial** | Sem libs externas. Reordenar fica out-of-scope (backend não suporta). Backend endpoint já existe. |
| Vídeo | **URL externa whitelist (YouTube/Vimeo) + iframe sandbox** | Sem custos de storage. Validação dupla (backend regex + storefront `bypassSecurityTrustResourceUrl`). |
| Form criar/editar | **Mesma page, modo via rota; criar redireciona pra editar pra habilitar mídia** | Imagens precisam de produto persistido (precisa ID). Evita estado "produto em rascunho". |
| Endpoint de reordenar imagens | **Não implementar nesta fase** | Backlog. Ordem FIFO suficiente. |
| Bulk actions | **Não implementar nesta fase** | YAGNI; raramente operadores selecionam 50+ produtos. |
| Permissões | **`roleGuard(['ROLE_ADMIN_LOJA','ROLE_GERENTE'])` nas rotas de catálogo** | Operadores só leem; backend já enforce. Frontend pode mostrar leitura sem bloquear ações que o backend vai negar. |

## 3. Arquitetura backend

### 3.1 Flyway V18

Caminho: `backend/application/src/main/resources/db/migration/V18__add_video_url_to_produto.sql`

```sql
ALTER TABLE produtos
    ADD COLUMN video_url VARCHAR(500);
```

Sem default — null = sem vídeo.

### 3.2 Entidade `Produto`

Adicionar em `backend/catalog/src/main/java/com/alispnor/pethub/catalog/domain/entity/Produto.java`:

```java
@Column(name = "video_url", length = 500)
private String videoUrl;
```

### 3.3 Validador `@ValidVideoEmbedUrl`

Caminho: `backend/common/src/main/java/com/alispnor/pethub/common/validation/ValidVideoEmbedUrl.java`

```java
@Documented
@Constraint(validatedBy = ValidVideoEmbedUrlValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidVideoEmbedUrl {
    String message() default "URL de vídeo deve ser do YouTube ou Vimeo";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

```java
public class ValidVideoEmbedUrlValidator implements ConstraintValidator<ValidVideoEmbedUrl, String> {
    private static final Pattern PADRAO = Pattern.compile(
        "^https://(www\\.)?(youtube\\.com/watch\\?v=[\\w-]+|youtu\\.be/[\\w-]+|vimeo\\.com/\\d+)([&?][\\w-=&]*)?$"
    );

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext context) {
        if (valor == null || valor.isBlank()) {
            return true;     // null/empty é permitido — opcional
        }
        if (valor.length() > 500) {
            return false;
        }
        return PADRAO.matcher(valor).matches();
    }
}
```

**Defesa anti-SSRF:** backend **nunca** faz request HTTP para esta URL. Apenas armazena e devolve. Storefront monta o `<iframe>` cliente-side com sandbox.

### 3.4 DTOs

`ProdutoCriarRequest`:
```java
@ValidVideoEmbedUrl
@Size(max = 500)
String videoUrl
```

`ProdutoAtualizarRequest`: idem.

`ProdutoResponse` e `ProdutoDetailResponse`: ganham `String videoUrl` no record.

### 3.5 MapStruct

`ProdutoMapper` ganha mapping `videoUrl → videoUrl` (default — sem custom mapping necessário).

## 4. Arquitetura frontend admin

### 4.1 Estrutura de arquivos novos

```
frontend/admin/src/app/modules/catalog/
├── models/
│   ├── produto.ts                       (ProdutoResponse, ProdutoCriarRequest, ProdutoAtualizarRequest,
│   │                                     ProdutoImagem, Origem, regex YouTube/Vimeo)
│   └── categoria.ts                     (CategoriaResponse, CategoriaCriarRequest, CategoriaAtualizarRequest,
│                                         CategoriaArvoreNode + helper para flatten)
├── services/
│   ├── produto.service.ts               (list/get/criar/atualizar/atualizarPreco/uploadImagem/removerImagem)
│   └── categoria.service.ts             (list/criar/atualizar/remover + montarArvore)
├── pages/
│   ├── produtos-lista/
│   │   ├── produtos-lista.page.{ts,html,scss}
│   ├── produto-form/
│   │   ├── produto-form.page.{ts,html,scss}
│   └── categorias/
│       ├── categorias.page.{ts,html,scss}
└── components/
    ├── image-drop-zone/
    │   └── image-drop-zone.component.{ts,html,scss}
    └── video-url-input/
        └── video-url-input.component.{ts,html,scss}
```

### 4.2 Models

`produto.ts`:

```ts
export type Origem = 'NACIONAL' | 'IMPORTADO' | 'NACIONAL_FABRICACAO_PROPRIA';

export interface ProdutoImagem {
  id: number;
  url: string;
  ordem: number;
  principal: boolean;
}

export interface ProdutoResponse {
  id: number;
  sku: string;
  nome: string;
  descricaoCurta: string | null;
  descricaoCompleta: string | null;
  marca: string | null;
  categoriaId: number;
  categoriaNome: string;
  pesoKg: number;
  alturaCm: number | null;
  larguraCm: number | null;
  profundidadeCm: number | null;
  ncm: string;
  origem: Origem;
  precoAtual: number;
  estoqueAtual: number;
  videoUrl: string | null;
  imagens: ProdutoImagem[];
  ativo: boolean;
  criadoEm: string;
}

export interface ProdutoCriarRequest {
  sku: string;
  nome: string;
  descricaoCurta?: string;
  descricaoCompleta?: string;
  marca?: string;
  categoriaId: number;
  pesoKg: number;
  alturaCm?: number;
  larguraCm?: number;
  profundidadeCm?: number;
  ncm: string;
  origem: Origem;
  precoInicial: number;
  videoUrl?: string;
}

export type ProdutoAtualizarRequest = Partial<Omit<ProdutoCriarRequest, 'sku' | 'precoInicial'>> & {
  ativo?: boolean;
};

export const REGEX_VIDEO_URL = /^https:\/\/(www\.)?(youtube\.com\/watch\?v=[\w-]+|youtu\.be\/[\w-]+|vimeo\.com\/\d+)([&?][\w-=&]*)?$/;

export const ORIGEM_LABEL: Record<Origem, string> = {
  NACIONAL: 'Nacional',
  IMPORTADO: 'Importado',
  NACIONAL_FABRICACAO_PROPRIA: 'Nacional (fabricação própria)',
};
```

`categoria.ts`:

```ts
export interface CategoriaResponse {
  id: number;
  nome: string;
  slug: string;
  descricao: string | null;
  categoriaPaiId: number | null;
  ativo: boolean;
  ordem: number;
}

export interface CategoriaCriarRequest {
  nome: string;
  slug: string;
  descricao?: string;
  categoriaPaiId?: number | null;
  ordem?: number;
}

export type CategoriaAtualizarRequest = Partial<CategoriaCriarRequest> & { ativo?: boolean };

export interface CategoriaArvoreNode {
  categoria: CategoriaResponse;
  filhas: CategoriaArvoreNode[];
  nivel: number;
}

/**
 * Constrói árvore a partir de lista plana. Categorias órfãs (pai não na lista)
 * viram raízes pra não sumir da UI.
 */
export function construirArvore(planas: CategoriaResponse[]): CategoriaArvoreNode[] {
  const indice = new Map<number, CategoriaArvoreNode>();
  planas.forEach(c => indice.set(c.id, { categoria: c, filhas: [], nivel: 0 }));

  const raizes: CategoriaArvoreNode[] = [];
  planas.forEach(c => {
    const node = indice.get(c.id)!;
    if (c.categoriaPaiId && indice.has(c.categoriaPaiId)) {
      const pai = indice.get(c.categoriaPaiId)!;
      node.nivel = pai.nivel + 1;
      pai.filhas.push(node);
    } else {
      raizes.push(node);
    }
  });

  const ordenarRec = (nodes: CategoriaArvoreNode[]) => {
    nodes.sort((a, b) => a.categoria.ordem - b.categoria.ordem);
    nodes.forEach(n => ordenarRec(n.filhas));
  };
  ordenarRec(raizes);
  return raizes;
}

/**
 * Achata árvore em lista plana mantendo ordem de DFS — usado pra renderizar
 * a lista indentada e pro `<select>` de pai.
 */
export function achatarArvore(raizes: CategoriaArvoreNode[]): CategoriaArvoreNode[] {
  const resultado: CategoriaArvoreNode[] = [];
  const visitar = (node: CategoriaArvoreNode) => {
    resultado.push(node);
    node.filhas.forEach(visitar);
  };
  raizes.forEach(visitar);
  return resultado;
}

/**
 * Lista de IDs que são `node` ou descendentes — usado para filtrar
 * opções no select de pai (evita ciclo no client antes do backend).
 */
export function descendentesId(node: CategoriaArvoreNode): Set<number> {
  const set = new Set<number>();
  const visitar = (n: CategoriaArvoreNode) => {
    set.add(n.categoria.id);
    n.filhas.forEach(visitar);
  };
  visitar(node);
  return set;
}
```

### 4.3 Services

`produto.service.ts`:

```ts
@Injectable({ providedIn: 'root' })
export class ProdutoService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBase;

  list(params: {
    q?: string;
    categoriaId?: number;
    ativo?: boolean;
    page?: number;
    size?: number;
    sort?: string;
  } = {}): Observable<PageResponse<ProdutoResponse>> {
    let httpParams = new HttpParams();
    if (params.q) httpParams = httpParams.set('q', params.q);
    if (params.categoriaId !== undefined) httpParams = httpParams.set('categoriaId', params.categoriaId);
    if (params.ativo !== undefined) httpParams = httpParams.set('ativo', params.ativo);
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size);
    if (params.sort) httpParams = httpParams.set('sort', params.sort);
    return this.http.get<PageResponse<ProdutoResponse>>(`${this.api}/catalog/products`, { params: httpParams });
  }

  bySku(sku: string): Observable<ProdutoResponse> {
    return this.http.get<ProdutoResponse>(`${this.api}/catalog/products/${sku}`);
  }

  criar(req: ProdutoCriarRequest): Observable<ProdutoResponse> {
    return this.http.post<ProdutoResponse>(`${this.api}/admin/catalog/products`, req);
  }

  atualizar(id: number, req: ProdutoAtualizarRequest): Observable<ProdutoResponse> {
    return this.http.put<ProdutoResponse>(`${this.api}/admin/catalog/products/${id}`, req);
  }

  atualizarPreco(id: number, preco: number): Observable<ProdutoResponse> {
    return this.http.post<ProdutoResponse>(`${this.api}/admin/catalog/products/${id}/price`, { preco });
  }

  uploadImagem(id: number, file: File, principal = false): Observable<ProdutoImagem> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('principal', String(principal));
    return this.http.post<ProdutoImagem>(`${this.api}/admin/catalog/products/${id}/images`, formData);
  }

  removerImagem(produtoId: number, imagemId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/admin/catalog/products/${produtoId}/images/${imagemId}`);
  }
}
```

`categoria.service.ts`:

```ts
@Injectable({ providedIn: 'root' })
export class CategoriaService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBase;

  list(): Observable<CategoriaResponse[]> {
    return this.http.get<CategoriaResponse[]>(`${this.api}/admin/catalog/categories`);
  }

  criar(req: CategoriaCriarRequest): Observable<CategoriaResponse> {
    return this.http.post<CategoriaResponse>(`${this.api}/admin/catalog/categories`, req);
  }

  atualizar(id: number, req: CategoriaAtualizarRequest): Observable<CategoriaResponse> {
    return this.http.put<CategoriaResponse>(`${this.api}/admin/catalog/categories/${id}`, req);
  }

  remover(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/admin/catalog/categories/${id}`);
  }
}
```

### 4.4 Routing

Em `app.routes.ts`, adicionar como filhas do `AdminShellPage`:

```ts
{
  path: 'produtos',
  canActivate: [roleGuard(['ROLE_ADMIN_LOJA', 'ROLE_GERENTE', 'ROLE_OPERADOR'])],
  loadComponent: () => import('@modules/catalog/pages/produtos-lista/produtos-lista.page')
    .then(m => m.ProdutosListaPage),
},
{
  path: 'produtos/novo',
  canActivate: [roleGuard(['ROLE_ADMIN_LOJA', 'ROLE_GERENTE'])],
  loadComponent: () => import('@modules/catalog/pages/produto-form/produto-form.page')
    .then(m => m.ProdutoFormPage),
},
{
  path: 'produtos/:sku/editar',
  canActivate: [roleGuard(['ROLE_ADMIN_LOJA', 'ROLE_GERENTE'])],
  loadComponent: () => import('@modules/catalog/pages/produto-form/produto-form.page')
    .then(m => m.ProdutoFormPage),
},
{
  path: 'categorias',
  canActivate: [roleGuard(['ROLE_ADMIN_LOJA', 'ROLE_GERENTE', 'ROLE_OPERADOR'])],
  loadComponent: () => import('@modules/catalog/pages/categorias/categorias.page')
    .then(m => m.CategoriasPage),
},
```

Observador: operador entra em `/produtos` (read-only); botões de criar/editar bloqueados via `roleGuard` na sub-rota. Sidebar do shell mostra item "Catálogo" habilitado pra todos os 3 perfis.

### 4.5 AdminShellPage update

Em `core/layout/admin-shell/admin-shell.page.ts`, atualizar `itensMenu`:

```ts
readonly itensMenu: ItemMenu[] = [
  { rota: '/dashboard',     rotulo: 'Dashboard',     icone: '📊', habilitada: true, exact: true },
  { rota: '/produtos',      rotulo: 'Catálogo',      icone: '📦', habilitada: true },
  { rota: '/categorias',    rotulo: 'Categorias',    icone: '🗂️', habilitada: true },
  { rota: '/pedidos',       rotulo: 'Pedidos',       icone: '📋', habilitada: false },
  { rota: '/comercial',     rotulo: 'Comercial',     icone: '🏷️', habilitada: false },
  { rota: '/clientes',      rotulo: 'Clientes',      icone: '👥', habilitada: false },
  { rota: '/relatorios',    rotulo: 'Relatórios',    icone: '📈', habilitada: false },
  { rota: '/configuracoes', rotulo: 'Configurações', icone: '⚙️', habilitada: false },
];
```

## 5. Páginas — comportamento

### 5.1 `ProdutosListaPage` (`/admin/produtos`)

Filtros sync com URL (`?q=...&categoriaId=...&ativo=...&page=0&sort=...`). Mudanças nos filtros: debounce 250ms na busca, change imediato no select de categoria/ativo, push de query params via `router.navigate([], { queryParamsHandling: 'merge' })`.

Tabela com colunas: imagem principal (thumbnail 48px), nome+SKU, categoria, preço, estoque, ativo badge, ações. Cabeçalho clicável em Nome/Preço/Estoque alterna `sort`.

Estados: skeleton durante load inicial, banner de erro com retry, empty state com CTA "Adicionar primeiro produto".

Paginação: `« Anterior | Página X de Y | Próximo »` no rodapé.

CTA `[+ Novo produto]` → `router.navigate(['/produtos/novo'])`.

### 5.2 `ProdutoFormPage` (`/admin/produtos/novo` e `/admin/produtos/:sku/editar`)

`ngOnInit` detecta modo:
```ts
const sku = this.route.snapshot.paramMap.get('sku');
this.modo.set(sku ? 'editar' : 'novo');
if (sku) {
  this.produtoService.bySku(sku).subscribe(p => {
    this.produtoCarregado.set(p);
    this.formulario.patchValue(p);
  });
}
this.categoriaService.list().subscribe(...);
```

Form em 2 colunas (em md+):
- **Esquerda:** SKU (readonly em editar), Nome, Marca, Categoria (`<select>` indentado), NCM, Origem, Descrição curta+completa, Medidas (peso/altura/largura/profundidade), Preço (só em editar — botão "Atualizar preço" chama endpoint dedicado), checkbox Ativo.
- **Direita (só em modo editar):** `<app-image-drop-zone>` para imagens, `<app-video-url-input>` para vídeo URL.

Submit:
- Modo "novo": `produtoService.criar()` → success → `router.navigate(['/produtos', resp.sku, 'editar'])` + toast verde "Produto criado. Adicione imagens e vídeo".
- Modo "editar": `produtoService.atualizar(id, req)` → success → toast verde + reload do form com response.

Erro 422 mapeia `ProblemDetail.errors[]` pros campos via `form.get(campo)?.setErrors({ backend: msg })`.

### 5.3 `CategoriasPage` (`/admin/categorias`)

Padrão lista + form inline (espelha `EnderecosPage` da Fase 5).

Lista renderiza `achatarArvore(construirArvore(categorias))` com indentação `padding-left: {nivel * 1.5}rem` aplicada via `[style.padding-left.rem]`.

Cada item mostra: nome (com indent), slug em mono, badge ativo, contagem de filhas, botões Editar/Remover.

Form inline (criar OU editar):
- Nome (required, max 100)
- Slug (auto-gerado de `nome.toLowerCase().replace(/\s+/g, '-')`, editável)
- Descrição (textarea)
- Categoria pai (`<select>` com indented options; em modo editar exclui o próprio nó e descendentes via `descendentesId(node)`)
- Ordem (number, default 0)
- Ativo (checkbox)

Delete: `ConfirmDialogService.open({ titulo: 'Remover categoria?', acaoVariant: 'danger' })`. Backend rejeita 409 se tem produtos vinculados — toast vermelho mostra detail.

### 5.4 `ImageDropZoneComponent`

Input: `@Input() produtoId: number; @Input() imagens: ProdutoImagem[]`.
Output: `@Output() imagensAtualizadas = new EventEmitter<ProdutoImagem[]>();`.

```html
<div class="rounded-lg border-2 border-dashed border-graphite-300 p-6 text-center
            transition-colors"
     [class.border-coral-500]="arrastando()"
     [class.bg-coral-50]="arrastando()"
     (dragover)="$event.preventDefault(); arrastando.set(true)"
     (dragleave)="arrastando.set(false)"
     (drop)="aoSoltar($event)">
  <p class="text-sm text-graphite-600">Arraste imagens aqui ou</p>
  <label class="mt-2 btn-ghost text-sm py-1.5 px-3 cursor-pointer">
    Escolher arquivos
    <input type="file" accept="image/*" multiple class="hidden"
           (change)="aoSelecionarArquivos($event)" />
  </label>
  <p class="mt-2 text-xs text-graphite-500">JPG/PNG/WEBP, máx 5 MB cada</p>
</div>

<div *ngIf="imagens.length > 0" class="mt-4 grid grid-cols-3 md:grid-cols-4 gap-3">
  <div *ngFor="let img of imagens" class="relative aspect-square rounded-md overflow-hidden border border-graphite-200">
    <img [src]="img.url" alt="" class="w-full h-full object-cover" />
    <button type="button" (click)="remover(img)"
            class="absolute top-1 right-1 bg-red-600 text-white rounded-full w-6 h-6 flex items-center justify-center text-xs"
            aria-label="Remover imagem">✕</button>
    <span *ngIf="img.principal" class="absolute bottom-1 left-1 bg-emerald-600 text-white rounded-full px-1.5 py-0.5 text-xs">
      Principal
    </span>
  </div>
</div>
```

`aoSoltar(event)`:
- `event.preventDefault()`, `arrastando.set(false)`.
- Filtra `event.dataTransfer.files` por `type.startsWith('image/')`.
- Limita 5 MB cada client-side (toast vermelho se exceder, ignora o arquivo).
- Upload sequencial via `produtoService.uploadImagem(produtoId, file)` — `concatMap` no RxJS.
- A cada sucesso, append ao array via `imagensAtualizadas.emit([...imagens, novaImg])`.

Remoção: `ConfirmDialogService.open(...)` antes de chamar `produtoService.removerImagem(...)`.

### 5.5 `VideoUrlInputComponent`

Input: `@Input() valor: string | null;`. Output: `@Output() valorChange = new EventEmitter<string | null>()`.

```html
<input type="url" [value]="valor ?? ''"
       (input)="aoMudar($event)"
       (blur)="atualizarPreview()"
       placeholder="https://www.youtube.com/watch?v=..."
       class="w-full rounded-md border-graphite-300" />
<p *ngIf="erroValidacao()" class="text-xs text-red-600 mt-1">{{ erroValidacao() }}</p>
<div *ngIf="urlEmbed() as embed" class="mt-3 aspect-video rounded-lg overflow-hidden bg-graphite-100">
  <iframe [src]="embed | safeResourceUrl"
          sandbox="allow-scripts allow-same-origin allow-presentation"
          class="w-full h-full" allowfullscreen></iframe>
</div>
```

`aoMudar()`: emite valor cru no `valorChange`.
`atualizarPreview()`: valida regex; se válido, calcula `urlEmbed()`.

### 5.6 Helper `urlEmbed` (compartilhado entre admin e storefront)

Coloca em `shared/utils/video-embed.ts` em ambos os apps (duplicado, como decisão da slice 6.1):

```ts
export function urlEmbed(urlPublica: string | null): string | null {
  if (!urlPublica) return null;
  const youtubeWatch = urlPublica.match(/youtube\.com\/watch\?v=([\w-]+)/);
  if (youtubeWatch) return `https://www.youtube.com/embed/${youtubeWatch[1]}`;
  const youtubeShort = urlPublica.match(/youtu\.be\/([\w-]+)/);
  if (youtubeShort) return `https://www.youtube.com/embed/${youtubeShort[1]}`;
  const vimeo = urlPublica.match(/vimeo\.com\/(\d+)/);
  if (vimeo) return `https://player.vimeo.com/video/${vimeo[1]}`;
  return null;
}
```

## 6. Storefront — vídeo no PDP

### 6.1 `SafeResourceUrlPipe`

Caminho: `frontend/storefront/src/app/shared/pipes/safe-resource-url.pipe.ts`

```ts
@Pipe({ name: 'safeResourceUrl', standalone: true })
export class SafeResourceUrlPipe implements PipeTransform {
  private readonly sanitizer = inject(DomSanitizer);

  transform(value: string | null | undefined): SafeResourceUrl | null {
    if (!value) return null;
    return this.sanitizer.bypassSecurityTrustResourceUrl(value);
  }
}
```

### 6.2 Update `product-detail.page`

Em `product-detail.page.ts`:
- Adicionar imports `SafeResourceUrlPipe` e helper `urlEmbed`.
- Computed `videoEmbed = computed(() => urlEmbed(this.produto()?.videoUrl ?? null))`.

Em `product-detail.page.html`, após a seção de imagens, antes da descrição:

```html
<section *ngIf="videoEmbed() as embed" class="mt-10 max-w-reading">
  <h2 class="text-2xl text-graphite-900">Vídeo</h2>
  <div class="mt-4 aspect-video rounded-lg overflow-hidden bg-graphite-100">
    <iframe [src]="embed | safeResourceUrl"
            sandbox="allow-scripts allow-same-origin allow-presentation"
            class="w-full h-full" allowfullscreen
            referrerpolicy="strict-origin-when-cross-origin"
            title="Vídeo do produto"></iframe>
  </div>
</section>
```

### 6.3 Update modelo `ProdutoDetail`

Em `frontend/storefront/src/app/modules/catalog/models/catalog.ts`, adicionar `videoUrl: string | null` em `ProdutoDetail` (ou seja qual for o tipo retornado por `getProductBySku`).

## 7. Sequência de implementação sugerida

1. **Backend Flyway V18** — migração + restart backend + smoke `psql` que coluna existe.
2. **Backend entidade + DTOs + mapper** — `Produto.videoUrl`, request/response, MapStruct.
3. **Backend validator** — `@ValidVideoEmbedUrl` + ValidatorImpl + test manual com curl (válida + inválida).
4. **Frontend admin — models + services** — `produto.ts`, `categoria.ts`, `produto.service.ts`, `categoria.service.ts`.
5. **Frontend admin — sidebar update** — habilitar Catálogo + Categorias.
6. **Frontend admin — components shared** — `VideoUrlInputComponent`, `ImageDropZoneComponent`, helper `urlEmbed`, pipe `safeResourceUrl` (cópia no admin).
7. **Frontend admin — ProdutosListaPage** — tabela + filtros URL-sync + paginação.
8. **Frontend admin — ProdutoFormPage** — form 2 colunas com modo via rota.
9. **Frontend admin — CategoriasPage** — lista plana indentada + form inline.
10. **Frontend admin — routing** — rotas + roleGuard.
11. **Frontend storefront — pipe + modelo + PDP** — `SafeResourceUrlPipe`, atualizar `ProdutoDetail`, seção de vídeo.
12. **Smoke E2E + docs** — atualizar `ai-memory/roadmap/fase-6-pendencias.md` + ROADMAP.

## 8. Smoke checklist (validação manual no browser ao fim)

Backend rodando, storefront em :4242, admin em :4244.

- [ ] `psql` confirma coluna `video_url` em `produtos`.
- [ ] `curl POST /api/v1/admin/catalog/products` com `videoUrl="https://malicious.com"` → 422 com mensagem do validador.
- [ ] `curl POST /api/v1/admin/catalog/products` com `videoUrl="https://www.youtube.com/watch?v=abc123"` → 201.
- [ ] Login admin em http://127.0.0.1:4244, clicar "Catálogo" → lista carrega com produtos do seed.
- [ ] Filtrar por categoria → URL ganha `?categoriaId=X`, lista recarrega.
- [ ] Sort por preço asc/desc funciona.
- [ ] Paginação funciona.
- [ ] Clicar "Novo produto" → form, SKU editável. Submeter → redireciona pra `/produtos/:sku/editar` com toast verde.
- [ ] No modo editar, dropar 2 imagens → upload sequencial, preview aparece, principal marcada.
- [ ] Remover imagem com confirm dialog.
- [ ] Colar URL inválida no input de vídeo → erro inline + sem preview.
- [ ] Colar URL YouTube válida → preview iframe abaixo.
- [ ] Salvar produto → toast verde.
- [ ] Abrir storefront http://127.0.0.1:4242/produtos/SKU → vídeo renderiza no PDP.
- [ ] Em outro produto sem `videoUrl` → seção de vídeo não aparece.
- [ ] `/admin/categorias`: criar categoria filha, ver indentação. Tentar criar com pai=descendente próprio → bloqueado client+backend.
- [ ] Login com operador (`operador@pethub.com`) → vê `/admin/produtos` (read-only); botão `+ Novo produto` redireciona via roleGuard pra `/dashboard`.

## 9. Dívida técnica registrada

- Testes formais Karma/Jest — continua mesma dívida das fases 1-6.1.
- Endpoint backend de reordenar imagens — backlog (slice 6.5 ou quando justificar).
- Bulk actions na lista de produtos — backlog.
- Upload de vídeo interno (arquivo) — backlog (storage/CDN).
- DnD reorder de categorias — backlog.
- Audit log — entra junto com Slice 6.3.

## 10. Critérios de "pronto"

- [ ] Flyway V18 aplicada.
- [ ] Backend build verde.
- [ ] `frontend/admin/` build verde.
- [ ] `frontend/storefront/` build verde.
- [ ] Smoke checklist §8 verde (manual).
- [ ] Commits Conventional Commits com escopo `feat(admin):`, `feat(backend):`, `feat(storefront):` ou `chore(*)` (~12-14 commits previstos).
- [ ] `ai-memory/roadmap/fase-6-pendencias.md` atualizado.
- [ ] `ROADMAP.md` continua 🚧 (slice 6.2 ✅ — atualiza nota).

## 11. Premissas técnicas

- Backend continua em `http://localhost:8080`, Postgres 5433, Redis 6380.
- Storefront em `:4242`, admin em `:4244`.
- Backend já tem endpoints REST de catálogo admin (confirmado): list, get-by-sku, criar, atualizar, atualizar preço, upload imagem, remover imagem, list categorias (admin), criar/atualizar/remover categoria.
- Backend já valida `categoriaPai` para evitar ciclo (defensivamente — não confirmado por leitura; assume comportamento via service do `CategoriaService` quando tiver `validarCiclo()`. Se não tiver, frontend é a única barreira nesta fase e fica como dívida).
- Credenciais admin do seed: `admin@pethub.com / Admin@123`, `gerente@pethub.com / Gerente@123`, `operador@pethub.com / Operador@123`.

## 🛡️ OWASP & Security Checkpoint

| Vulnerabilidade OWASP | Mitigação aplicada neste slice |
|---|---|
| **A01:2021 — Broken Access Control** | `roleGuard(['ROLE_ADMIN_LOJA','ROLE_GERENTE'])` em rotas de mutation (`/produtos/novo`, `/produtos/:sku/editar`). Backend já enforce via `@PreAuthorize` em todos os endpoints admin. Operador entra na lista mas não nas ações. |
| **A03:2021 — Injection (XSS via vídeo embed)** | URL de vídeo aceita apenas YouTube/Vimeo (regex whitelist no validator backend + regex no input client). `<iframe sandbox="allow-scripts allow-same-origin allow-presentation">` no storefront restringe o que o conteúdo embedded pode fazer. `referrerpolicy="strict-origin-when-cross-origin"` mitiga vazamento. `bypassSecurityTrustResourceUrl` é seguro porque o backend já validou contra whitelist. |
| **A03:2021 — Injection (PostgreSQL)** | Tudo via JPA/MapStruct — sem SQL concatenado. Nova coluna `video_url` é `VARCHAR(500)` com tamanho limitado. |
| **A04:2021 — Insecure Design** | Validação dupla anti-SSRF: backend NUNCA faz HTTP request à URL do vídeo (só armazena); regex limita providers conhecidos (YouTube/Vimeo); sandbox no iframe. Reordenar imagens não implementado para evitar implementação pela metade. |
| **A05:2021 — Security Misconfiguration** | iframe com sandbox restritivo. CORS já cobre admin :4244 (slice 6.1). Headers de segurança (CSP `frame-src https://www.youtube.com https://player.vimeo.com`) ficam pro nginx em prod. |
| **A08:2021 — Software & Data Integrity Failures** | Sem dependências externas adicionadas — todos os componentes (drop zone, video input) são código próprio. |
| **A10:2021 — SSRF** | Backend nunca faz request à `videoUrl`. Apenas armazena após validação regex. Risco zero. |
| **API1:2023 — BOLA** | Endpoints já enforce ownership (admin = global, não exige ownership por loja nesta fase single-tenant). |
| **API3:2023 — BOPLA** | `ProdutoResponse` não expõe campos sensíveis. `ProdutoImagem` retorna URL pública (servida via `/files` proxy do backend). |

**Premissas de infra:**
- HTTPS em prod (admin via subdomínio `admin.pethub.com` ou subpath `/admin/`).
- `Content-Security-Policy` no nginx em prod inclui `frame-src https://www.youtube.com https://player.vimeo.com` para o iframe funcionar.
- `Permissions-Policy: fullscreen=(self "https://www.youtube.com" "https://player.vimeo.com")` para `allowfullscreen` funcionar em prod.
