package com.alispnor.pethub.cart.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateQtyRequest(
        @Min(1) @Max(999) int qty
) {
}
