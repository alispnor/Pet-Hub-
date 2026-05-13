package com.alispnor.pethub.pricing.application.dto;

import com.alispnor.pethub.cart.domain.CartItem;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado do cálculo de preços para preview/place-order.
 * Ordem de aplicação:
 *   1. subtotal = soma(precoUnitario × qty)
 *   2. desconto_promocoes = maior desconto encontrado por item (não acumula)
 *   3. desconto_cupom = aplicado sobre (subtotal − desconto_promocoes)
 *   4. impostos = informativos (já embutidos no preço para PF)
 *   5. valor_total = (subtotal − descontos) + frete
 */
public record PricingBreakdown(
        List<CartItem> itens,
        BigDecimal subtotal,
        BigDecimal descontoPromocoes,
        BigDecimal descontoCupom,
        ImpostosCalculados impostos,
        BigDecimal valorFrete,
        BigDecimal valorTotal,
        String cupomCodigo
) {
}
