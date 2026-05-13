package com.alispnor.pethub.shipping.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

public record ShippingItemRequest(
        @NotBlank String sku,
        @Min(1) @Max(999) int qty
) implements Serializable {
}
