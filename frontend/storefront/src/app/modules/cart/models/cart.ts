/**
 * Espelha os records do backend (cart/domain/*.java).
 * BigDecimal vira number aqui; sempre formatar com pipe `currency`.
 */
export interface CartItem {
  produtoId: number;
  sku: string;
  nome: string;
  imagemUrl: string | null;
  qty: number;
  precoUnitario: number;
  subtotal: number;
}

export interface CartCoupon {
  codigo: string;
  descontoAplicado: number;
}

export interface CartResponse {
  userId: number;
  items: CartItem[];
  cupom: CartCoupon | null;
  subtotal: number;
  totalItens: number;
  atualizadoEm: string;        // ISO LocalDateTime
}

export interface AddItemRequest {
  sku: string;
  qty: number;
}

export interface UpdateQtyRequest {
  qty: number;
}

export interface ApplyCouponRequest {
  codigo: string;
}
