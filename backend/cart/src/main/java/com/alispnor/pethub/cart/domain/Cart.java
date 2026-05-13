package com.alispnor.pethub.cart.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public record Cart(
        Long userId,
        List<CartItem> items,
        CartCoupon cupom,
        LocalDateTime atualizadoEm
) {

    public Cart {
        items = items == null ? new ArrayList<>() : new ArrayList<>(items);
    }

    public static Cart empty(Long userId) {
        return new Cart(userId, List.of(), null, LocalDateTime.now());
    }

    public Cart withItems(List<CartItem> newItems) {
        return new Cart(userId, newItems, cupom, LocalDateTime.now());
    }

    public Cart withCupom(CartCoupon newCupom) {
        return new Cart(userId, items, newCupom, LocalDateTime.now());
    }

    public BigDecimal subtotal() {
        return items.stream()
                .map(CartItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
