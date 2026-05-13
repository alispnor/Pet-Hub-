package com.alispnor.pethub.cart.domain;

import java.math.BigDecimal;

public record CartCoupon(
        String codigo,
        BigDecimal descontoAplicado
) {
}
