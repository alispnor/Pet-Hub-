package com.alispnor.pethub.customer.infrastructure.rest;

import com.alispnor.pethub.common.dto.PageableResponse;
import com.alispnor.pethub.customer.application.dto.AdminCustomerDetail;
import com.alispnor.pethub.customer.application.dto.AdminCustomerSummary;
import com.alispnor.pethub.customer.application.usecase.AdminCustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/customers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN_LOJA','GERENTE')")
@Tag(name = "Admin — Clientes", description = "Consulta de clientes para a operação. PII mascarada; cartões nunca expostos.")
public class AdminCustomerController {

    private final AdminCustomerService adminCustomerService;

    @GetMapping
    @Operation(summary = "Lista clientes com email mascarado (busca opcional em nome/email)")
    public PageableResponse<AdminCustomerSummary> listar(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("dataCadastro").descending());
        return adminCustomerService.listar(q, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um cliente sem CPF completo, sem cartões e com email/telefone mascarados")
    public AdminCustomerDetail detalhar(@PathVariable Long id) {
        return adminCustomerService.detalhar(id);
    }
}
