package com.alispnor.pethub.catalog.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DefinirPrecoRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal valor
) {
}
