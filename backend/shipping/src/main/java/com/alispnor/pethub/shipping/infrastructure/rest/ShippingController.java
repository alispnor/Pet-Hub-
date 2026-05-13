package com.alispnor.pethub.shipping.infrastructure.rest;

import com.alispnor.pethub.shipping.application.dto.OpcaoFreteResponse;
import com.alispnor.pethub.shipping.application.dto.ShippingCalculateRequest;
import com.alispnor.pethub.shipping.application.usecase.ShippingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shipping")
@RequiredArgsConstructor
@Tag(name = "Shipping", description = "Cálculo de frete (cached por 1h em Redis)")
public class ShippingController {

    private final ShippingService shippingService;

    @PostMapping("/calculate")
    @Operation(summary = "Calcula opções de frete a partir do CEP destino e dos itens (sku+qty)")
    public List<OpcaoFreteResponse> calcular(@Valid @RequestBody ShippingCalculateRequest request) {
        return shippingService.calcular(request);
    }
}
