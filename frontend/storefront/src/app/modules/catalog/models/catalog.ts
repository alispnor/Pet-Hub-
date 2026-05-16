export type Origem = 'NACIONAL' | 'IMPORTADO';

export interface ProdutoImagem {
  id: number;
  url: string;
  alt: string;
  ordem: number;
  principal: boolean;
}

export interface Categoria {
  id: number;
  nome: string;
  slug: string;
  descricao?: string | null;
  categoriaPaiId?: number | null;
  ativo: boolean;
  ordem: number;
}

export interface ProdutoSummary {
  id: number;
  sku: string;
  nome: string;
  descricaoCurta?: string | null;
  preco: number;
  imagemPrincipal?: string | null;
  categoriaSlug?: string | null;
  destacado: boolean;
}

export interface ProdutoDetail {
  id: number;
  sku: string;
  nome: string;
  descricaoCurta?: string | null;
  descricaoCompleta?: string | null;
  marca?: string | null;
  ncm?: string | null;
  origem?: Origem | null;
  preco: number;
  pesoKg?: number | null;
  alturaCm?: number | null;
  larguraCm?: number | null;
  profundidadeCm?: number | null;
  specs?: Record<string, unknown> | null;
  imagens: ProdutoImagem[];
  categoria: Categoria;
  ativo: boolean;
  destacado: boolean;
}
