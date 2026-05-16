import { CartItem } from '@modules/cart/models/cart';
import { OpcaoFrete } from '@shared/models/shipping';

/** Quebra de impostos retornada pelo backend (pricing/dto/ImpostosCalculados). */
export interface ImpostosCalculados {
  valorImpostos: number;
  detalhes: Array<{ ncm: string; uf: string; aliquotaPercentual: number; valor: number }>;
}

export interface CheckoutPreviewRequest {
  enderecoEntregaId: number;
  opcaoFreteCodigo: string;
  cupom?: string | null;
}

export interface CheckoutPreviewResponse {
  itens: CartItem[];
  subtotal: number;
  descontoPromocoes: number;
  descontoCupom: number;
  impostosCalculados: ImpostosCalculados;
  freteEscolhido: OpcaoFrete;
  valorTotal: number;
  cupomCodigo: string | null;
}

export type PaymentMethodKind = 'CARTAO_CREDITO' | 'CARTAO_DEBITO' | 'PIX' | 'BOLETO';
export type PaymentStatus = 'PROCESSING' | 'APPROVED' | 'REJECTED' | 'REFUNDED';

export interface PlaceOrderRequest {
  enderecoEntregaId: number;
  enderecoCobrancaId: number;
  opcaoFreteCodigo: string;
  formaPagamentoId: number;
  cupom?: string | null;
  parcelas?: number | null;
  idempotencyKey: string;
}

export interface PlaceOrderResponse {
  tentativaPagamentoId: number;
  referenciaPedido: string;
  pedidoId: number | null;
  metodo: PaymentMethodKind;
  status: PaymentStatus;
  valorTotal: number;
  gatewayTransactionId: string | null;
  qrCode: string | null;
  boletoUrl: string | null;
}

/**
 * Stored in sessionStorage between wizard steps.
 * Bump the storage key to v2 when this shape changes incompatibly.
 */
export interface CheckoutState {
  enderecoEntregaId: number | null;
  enderecoCobrancaId: number | null;
  opcaoFreteCodigo: string | null;
  opcaoFreteSnapshot: OpcaoFrete | null;
  formaPagamentoId: number | null;
  parcelas: number;
  idempotencyKey: string | null;
}

export const EMPTY_CHECKOUT_STATE: CheckoutState = {
  enderecoEntregaId: null,
  enderecoCobrancaId: null,
  opcaoFreteCodigo: null,
  opcaoFreteSnapshot: null,
  formaPagamentoId: null,
  parcelas: 1,
  idempotencyKey: null,
};
