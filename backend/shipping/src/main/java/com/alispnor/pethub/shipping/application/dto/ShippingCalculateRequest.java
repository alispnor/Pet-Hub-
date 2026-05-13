package com.alispnor.pethub.shipping.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.io.Serializable;
import java.util.List;

public record ShippingCalculateRequest(
        @Pattern(regexp = "\\d{8}", message = "CEP deve ter 8 dígitos numéricos") String cepDestino,
        @NotEmpty @Valid List<ShippingItemRequest> itens
) implements Serializable {
}
