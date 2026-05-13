package com.alispnor.pethub.checkout.application.dto;

import com.alispnor.pethub.cart.domain.CartItem;
import com.alispnor.pethub.pricing.application.dto.ImpostosCalculados;
import com.alispnor.pethub.shipping.application.dto.OpcaoFreteResponse;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutPreviewResponse(
        List<CartItem> itens,
        BigDecimal subtotal,
        BigDecimal descontoPromocoes,
        BigDecimal descontoCupom,
        ImpostosCalculados impostosCalculados,
        OpcaoFreteResponse freteEscolhido,
        BigDecimal valorTotal,
        String cupomCodigo
) {
}
