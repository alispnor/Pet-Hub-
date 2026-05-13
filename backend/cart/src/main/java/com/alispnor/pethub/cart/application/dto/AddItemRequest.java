package com.alispnor.pethub.cart.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddItemRequest(
        @NotBlank @Size(max = 50) String sku,
        @Min(1) @Max(999) int qty
) {
}
