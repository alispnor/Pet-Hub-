package com.alispnor.pethub.customer.infrastructure.rest;

import com.alispnor.pethub.customer.application.dto.CreateFormaPagamentoRequest;
import com.alispnor.pethub.customer.application.dto.FormaPagamentoResponse;
import com.alispnor.pethub.customer.application.dto.TokenizeCardRequest;
import com.alispnor.pethub.customer.application.dto.TokenizeCardResponse;
import com.alispnor.pethub.customer.application.usecase.FormaPagamentoService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Formas de pagamento", description = "Cartões tokenizados, PIX e boleto do cliente autenticado")
public class FormaPagamentoController {

    private final FormaPagamentoService formaPagamentoService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/payment-methods/tokenize")
    @Operation(summary = "Tokeniza um cartão via gateway (mock). Em produção o frontend chama o gateway direto.")
    public TokenizeCardResponse tokenizar(@Valid @RequestBody TokenizeCardRequest request) {
        return formaPagamentoService.tokenizar(request);
    }

    @GetMapping("/me/payment-methods")
    @Operation(summary = "Lista as formas de pagamento do cliente autenticado")
    public List<FormaPagamentoResponse> listar() {
        var current = currentUserProvider.requireCurrent();
        return formaPagamentoService.listar(current.id());
    }

    @PostMapping("/me/payment-methods")
    @Operation(summary = "Cadastra uma forma de pagamento (cartão exige gatewayToken; PIX/BOLETO só o tipo)")
    public ResponseEntity<FormaPagamentoResponse> criar(@Valid @RequestBody CreateFormaPagamentoRequest request) {
        var current = currentUserProvider.requireCurrent();
        var response = formaPagamentoService.criar(current.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/me/payment-methods/{id}")
    @Operation(summary = "Remove uma forma de pagamento")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        var current = currentUserProvider.requireCurrent();
        formaPagamentoService.remover(current.id(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/payment-methods/{id}/default")
    @Operation(summary = "Define a forma de pagamento como padrão (apenas uma por cliente)")
    public FormaPagamentoResponse definirPadrao(@PathVariable Long id) {
        var current = currentUserProvider.requireCurrent();
        return formaPagamentoService.definirPadrao(current.id(), id);
    }
}
