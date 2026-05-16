export interface ShippingItemRequest {
  sku: string;
  qty: number;
}

export interface ShippingCalculateRequest {
  cepDestino: string;
  itens: ShippingItemRequest[];
}

export interface OpcaoFrete {
  codigo: string;
  transportadora: string;
  servico: string;
  valor: number;
  prazoDias: number;
}
