export type StatusPedido =
  | 'PENDENTE_PAGAMENTO'
  | 'PAGAMENTO_APROVADO'
  | 'PAGAMENTO_REJEITADO'
  | 'SEPARACAO'
  | 'EM_TRANSPORTE'
  | 'ENTREGUE'
  | 'CANCELADO'
  | 'DEVOLVIDO';

export type StatusEtapa = 'CONCLUIDA' | 'ATUAL' | 'PENDENTE';

export interface EtapaTimeline {
  nome: string;
  status: StatusEtapa;
  ocorridoEm: string | null;
  tempoDecorrido: string | null;
  previsaoEntrega: string | null;
}

export interface Timeline {
  numeroPedido: string;
  statusAtual: StatusPedido;
  etapas: EtapaTimeline[];
}

export interface PedidoResumoResponse {
  id: number;
  numeroPedido: string;
  status: StatusPedido;
  valorTotal: number;
  formaPagamentoTipo: string;
  totalItens: number;
  criadoEm: string;
}

export interface PedidoItemResponse {
  skuProduto: string;
  nomeProduto: string;
  precoUnitario: number;
  quantidade: number;
  fotoUrl: string | null;
}

export interface EnderecoSnapshot {
  apelido?: string;
  logradouro: string;
  numero?: string;
  complemento?: string;
  bairro: string;
  cidade: string;
  uf: string;
  cep: string;
}

export interface OpcaoFreteSnapshot {
  codigo: string;
  transportadora: string;
  valor: number;
  prazoDias: number;
}

export interface PedidoResponse {
  id: number;
  numeroPedido: string;
  clienteId: number;
  status: StatusPedido;
  itens: PedidoItemResponse[];
  enderecoEntrega: EnderecoSnapshot;
  enderecoCobranca: EnderecoSnapshot;
  opcaoFrete: OpcaoFreteSnapshot;
  cupomCodigo: string | null;
  valorSubtotal: number;
  valorDescontos: number;
  valorImpostos: number;
  valorFrete: number;
  valorTotal: number;
  formaPagamentoTipo: string;
  formaPagamentoUltimos4: string | null;
  formaPagamentoBandeira: string | null;
  tentativaPagamentoId: number | null;
  observacoes: string | null;
  criadoEm: string;
  atualizadoEm: string;
}

export interface PageResponse<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export const STATUS_LABEL: Record<StatusPedido, string> = {
  PENDENTE_PAGAMENTO: 'Aguardando pagamento',
  PAGAMENTO_APROVADO: 'Pagamento aprovado',
  PAGAMENTO_REJEITADO: 'Pagamento rejeitado',
  SEPARACAO: 'Em separação',
  EM_TRANSPORTE: 'Em transporte',
  ENTREGUE: 'Entregue',
  CANCELADO: 'Cancelado',
  DEVOLVIDO: 'Devolvido',
};

export function ehStatusTerminal(status: StatusPedido): boolean {
  return status === 'ENTREGUE'
      || status === 'CANCELADO'
      || status === 'DEVOLVIDO'
      || status === 'PAGAMENTO_REJEITADO';
}

export function classesBadgeStatus(status: StatusPedido): string {
  switch (status) {
    case 'ENTREGUE':
    case 'PAGAMENTO_APROVADO':
      return 'bg-emerald-100 text-emerald-800';
    case 'CANCELADO':
    case 'PAGAMENTO_REJEITADO':
      return 'bg-red-100 text-red-800';
    case 'DEVOLVIDO':
      return 'bg-amber-100 text-amber-800';
    case 'SEPARACAO':
    case 'EM_TRANSPORTE':
      return 'bg-yellow-100 text-yellow-800';
    case 'PENDENTE_PAGAMENTO':
      return 'bg-blue-100 text-blue-800';
  }
}
