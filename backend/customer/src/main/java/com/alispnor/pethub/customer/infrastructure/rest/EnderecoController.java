package com.alispnor.pethub.customer.infrastructure.rest;

import com.alispnor.pethub.customer.application.dto.CreateEnderecoRequest;
import com.alispnor.pethub.customer.application.dto.EnderecoResponse;
import com.alispnor.pethub.customer.application.dto.SetDefaultRequest;
import com.alispnor.pethub.customer.application.dto.UpdateEnderecoRequest;
import com.alispnor.pethub.customer.application.dto.ViaCepResponse;
import com.alispnor.pethub.customer.application.usecase.EnderecoService;
import com.alispnor.pethub.identity.infrastructure.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Validated
@Tag(name = "Endereços", description = "Endereços do cliente autenticado e consulta de CEP via ViaCEP")
public class EnderecoController {

    private final EnderecoService enderecoService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/cep/{cep}")
    @Operation(summary = "Consulta um CEP no ViaCEP (cache Redis 24h)")
    public ViaCepResponse consultarCep(
            @PathVariable @Pattern(regexp = "\\d{8}", message = "CEP deve ter 8 dígitos numéricos") String cep) {
        return enderecoService.consultarCep(cep);
    }

    @GetMapping("/me/addresses")
    @Operation(summary = "Lista os endereços do cliente autenticado")
    public List<EnderecoResponse> listar() {
        var current = currentUserProvider.requireCurrent();
        return enderecoService.listar(current.id());
    }

    @PostMapping("/me/addresses")
    @Operation(summary = "Cria um novo endereço (o primeiro vira padrão de entrega e cobrança)")
    public ResponseEntity<EnderecoResponse> criar(@Valid @RequestBody CreateEnderecoRequest request) {
        var current = currentUserProvider.requireCurrent();
        var response = enderecoService.criar(current.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/me/addresses/{id}")
    @Operation(summary = "Atualiza um endereço")
    public EnderecoResponse atualizar(@PathVariable Long id, @Valid @RequestBody UpdateEnderecoRequest request) {
        var current = currentUserProvider.requireCurrent();
        return enderecoService.atualizar(current.id(), id, request);
    }

    @DeleteMapping("/me/addresses/{id}")
    @Operation(summary = "Remove um endereço")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        var current = currentUserProvider.requireCurrent();
        enderecoService.remover(current.id(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/addresses/{id}/default")
    @Operation(summary = "Define o endereço como padrão de entrega e/ou cobrança (apenas um pode estar marcado por vez)")
    public EnderecoResponse definirPadrao(@PathVariable Long id, @Valid @RequestBody SetDefaultRequest request) {
        var current = currentUserProvider.requireCurrent();
        return enderecoService.definirPadrao(current.id(), id, request);
    }
}
