package com.alispnor.pethub.checkout.infrastructure.rest;

import com.alispnor.pethub.checkout.application.dto.CheckoutPreviewRequest;
import com.alispnor.pethub.checkout.application.dto.CheckoutPreviewResponse;
import com.alispnor.pethub.checkout.application.usecase.CheckoutService;
import com.alispnor.pethub.identity.infrastructure.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/checkout")
@RequiredArgsConstructor
@Tag(name = "Checkout", description = "Preview e fechamento de pedido")
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/preview")
    @Operation(summary = "Calcula subtotal, descontos, impostos, frete e total para o pedido proposto sem persistir nada")
    public CheckoutPreviewResponse preview(@Valid @RequestBody CheckoutPreviewRequest request) {
        var current = currentUserProvider.requireCurrent();
        return checkoutService.preview(current.id(), request);
    }
}
