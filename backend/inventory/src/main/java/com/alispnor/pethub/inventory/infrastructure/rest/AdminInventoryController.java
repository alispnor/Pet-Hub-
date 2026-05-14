package com.alispnor.pethub.inventory.infrastructure.rest;

import com.alispnor.pethub.identity.infrastructure.security.CurrentUserProvider;
import com.alispnor.pethub.inventory.application.dto.EstoqueResponse;
import com.alispnor.pethub.inventory.application.dto.MovimentacaoManualRequest;
import com.alispnor.pethub.inventory.application.dto.MovimentacaoResponse;
import com.alispnor.pethub.inventory.application.usecase.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN_LOJA','GERENTE','OPERADOR')")
@Tag(name = "Admin · Inventory", description = "Gestão de estoque, reservas e movimentações")
public class AdminInventoryController {

    private final InventoryService inventoryService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    @Operation(summary = "Listagem paginada de todos os SKUs com estoque")
    public Page<EstoqueResponse> listar(Pageable pageable) {
        return inventoryService.listar(pageable);
    }

    @GetMapping("/low-stock")
    @Operation(summary = "SKUs com (quantidade - reservada) <= mínimo")
    public Page<EstoqueResponse> lowStock(Pageable pageable) {
        return inventoryService.listarLowStock(pageable);
    }

    @GetMapping("/{sku}")
    @Operation(summary = "Detalhe de estoque por SKU")
    public EstoqueResponse detalhe(@PathVariable String sku) {
        return inventoryService.detalheBySku(sku);
    }

    @GetMapping("/{sku}/history")
    @Operation(summary = "Histórico paginado de movimentações para um SKU")
    public Page<MovimentacaoResponse> historico(@PathVariable String sku, Pageable pageable) {
        return inventoryService.historicoBySku(sku, pageable);
    }

    @PostMapping("/movements")
    @PreAuthorize("hasAnyRole('ADMIN_LOJA','GERENTE')")
    @Operation(summary = "Movimentação manual de entrada, saída ou ajuste (não dispara workflow de pedido)")
    public MovimentacaoResponse movimentar(@Valid @RequestBody MovimentacaoManualRequest req) {
        var current = currentUserProvider.requireCurrent();
        return inventoryService.registrarMovimentacaoManual(current.id(), req);
    }
}
