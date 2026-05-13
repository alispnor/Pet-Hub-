package com.alispnor.pethub.cart.infrastructure.rest;

import com.alispnor.pethub.cart.application.dto.AddItemRequest;
import com.alispnor.pethub.cart.application.dto.ApplyCouponRequest;
import com.alispnor.pethub.cart.application.dto.CartResponse;
import com.alispnor.pethub.cart.application.dto.UpdateQtyRequest;
import com.alispnor.pethub.cart.application.usecase.CartService;
import com.alispnor.pethub.identity.infrastructure.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Carrinho de compras do cliente autenticado (Redis, TTL 30 dias)")
public class CartController {

    private final CartService cartService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    @Operation(summary = "Retorna o carrinho atual do cliente")
    public CartResponse obter() {
        var current = currentUserProvider.requireCurrent();
        return cartService.obter(current.id());
    }

    @PostMapping("/items")
    @Operation(summary = "Adiciona um item ao carrinho (acumula qty se o SKU já existir)")
    public ResponseEntity<CartResponse> adicionar(@Valid @RequestBody AddItemRequest request) {
        var current = currentUserProvider.requireCurrent();
        var response = cartService.adicionarItem(current.id(), request.sku(), request.qty());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/items/{sku}")
    @Operation(summary = "Atualiza a quantidade de um item já no carrinho")
    public CartResponse atualizar(@PathVariable String sku, @Valid @RequestBody UpdateQtyRequest request) {
        var current = currentUserProvider.requireCurrent();
        return cartService.atualizarItem(current.id(), sku, request);
    }

    @DeleteMapping("/items/{sku}")
    @Operation(summary = "Remove um item do carrinho")
    public CartResponse remover(@PathVariable String sku) {
        var current = currentUserProvider.requireCurrent();
        return cartService.removerItem(current.id(), sku);
    }

    @PostMapping("/coupon")
    @Operation(summary = "Anexa um código de cupom (validação real ocorre no checkout)")
    public CartResponse aplicarCupom(@Valid @RequestBody ApplyCouponRequest request) {
        var current = currentUserProvider.requireCurrent();
        return cartService.aplicarCupom(current.id(), request);
    }

    @DeleteMapping("/coupon")
    @Operation(summary = "Remove o cupom anexado")
    public CartResponse removerCupom() {
        var current = currentUserProvider.requireCurrent();
        return cartService.removerCupom(current.id());
    }

    @DeleteMapping
    @Operation(summary = "Limpa o carrinho")
    public ResponseEntity<Void> limpar() {
        var current = currentUserProvider.requireCurrent();
        cartService.limpar(current.id());
        return ResponseEntity.noContent().build();
    }
}
