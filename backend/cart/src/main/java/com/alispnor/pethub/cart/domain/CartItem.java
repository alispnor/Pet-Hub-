package com.alispnor.pethub.cart.domain;

import java.math.BigDecimal;

public record CartItem(
        Long produtoId,
        String sku,
        String nome,
        String imagemUrl,
        int qty,
        BigDecimal precoUnitario,
        BigDecimal subtotal
) {

    public CartItem withQty(int newQty) {
        return new CartItem(produtoId, sku, nome, imagemUrl, newQty, precoUnitario,
                precoUnitario.multiply(BigDecimal.valueOf(newQty)));
    }
}
