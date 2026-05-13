package com.alispnor.pethub.cart.application.dto;

import com.alispnor.pethub.cart.domain.Cart;
import com.alispnor.pethub.cart.domain.CartCoupon;
import com.alispnor.pethub.cart.domain.CartItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CartResponse(
        Long userId,
        List<CartItem> items,
        CartCoupon cupom,
        BigDecimal subtotal,
        int totalItens,
        LocalDateTime atualizadoEm
) {

    public static CartResponse from(Cart cart) {
        return new CartResponse(
                cart.userId(),
                List.copyOf(cart.items()),
                cart.cupom(),
                cart.subtotal(),
                cart.items().stream().mapToInt(CartItem::qty).sum(),
                cart.atualizadoEm()
        );
    }
}
