package com.alispnor.pethub.customer.infrastructure.rest;

import com.alispnor.pethub.customer.application.dto.PerfilResponse;
import com.alispnor.pethub.customer.application.dto.SetCpfRequest;
import com.alispnor.pethub.customer.application.dto.UpdatePerfilRequest;
import com.alispnor.pethub.customer.application.usecase.PerfilService;
import com.alispnor.pethub.identity.infrastructure.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers/me/profile")
@RequiredArgsConstructor
@Tag(name = "Perfil do cliente", description = "Dados do perfil (LGPD) do cliente autenticado")
public class PerfilController {

    private final PerfilService perfilService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    @Operation(summary = "Retorna o perfil do cliente autenticado")
    public PerfilResponse get() {
        var current = currentUserProvider.requireCurrent();
        return perfilService.getOrCreate(current.id());
    }

    @PutMapping
    @Operation(summary = "Atualiza dados do perfil (data nascimento, gênero, telefone, aceites)")
    public PerfilResponse update(@Valid @RequestBody UpdatePerfilRequest request) {
        var current = currentUserProvider.requireCurrent();
        return perfilService.update(current.id(), request);
    }

    @PostMapping("/cpf")
    @Operation(summary = "Define o CPF do cliente (operação separada por ser PII sensível e única)")
    public PerfilResponse setCpf(@Valid @RequestBody SetCpfRequest request) {
        var current = currentUserProvider.requireCurrent();
        return perfilService.setCpf(current.id(), request);
    }
}
