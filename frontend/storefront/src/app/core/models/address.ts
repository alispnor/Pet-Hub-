export type UnidadeFederativa =
  | 'AC' | 'AL' | 'AP' | 'AM' | 'BA' | 'CE' | 'DF' | 'ES' | 'GO' | 'MA'
  | 'MT' | 'MS' | 'MG' | 'PA' | 'PB' | 'PR' | 'PE' | 'PI' | 'RJ' | 'RN'
  | 'RS' | 'RO' | 'RR' | 'SC' | 'SP' | 'SE' | 'TO';

export type TipoEndereco = 'RESIDENCIAL' | 'COMERCIAL';

export interface EnderecoResponse {
  id: number;
  apelido: string;
  cep: string;
  logradouro: string;
  numero: string | null;
  complemento: string | null;
  bairro: string;
  cidade: string;
  uf: UnidadeFederativa;
  pais: string;
  tipo: TipoEndereco;
  padraoEntrega: boolean;
  padraoCobranca: boolean;
  ativo: boolean;
}

export interface CreateEnderecoRequest {
  apelido: string;
  cep: string;                   // 8 dígitos numéricos (sem máscara)
  logradouro: string;
  numero?: string;
  complemento?: string;
  bairro: string;
  cidade: string;
  uf: UnidadeFederativa;
  tipo: TipoEndereco;
  padraoEntrega?: boolean;
  padraoCobranca?: boolean;
}

export interface ViaCepResponse {
  cep: string;
  logradouro: string;
  complemento: string;
  bairro: string;
  cidade: string;
  uf: string;
  erro: boolean | null;
}

export const UFS: readonly UnidadeFederativa[] = [
  'AC','AL','AP','AM','BA','CE','DF','ES','GO','MA',
  'MT','MS','MG','PA','PB','PR','PE','PI','RJ','RN',
  'RS','RO','RR','SC','SP','SE','TO',
];
