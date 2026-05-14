package com.alispnor.pethub.order.infrastructure.rest;

import com.alispnor.pethub.identity.infrastructure.security.CurrentUserProvider;
import com.alispnor.pethub.order.application.dto.PedidoResponse;
import com.alispnor.pethub.order.application.dto.PedidoResumoResponse;
import com.alispnor.pethub.order.application.dto.TransicaoRequest;
import com.alispnor.pethub.order.application.usecase.OrderService;
import com.alispnor.pethub.order.domain.entity.AtorTipo;
import com.alispnor.pethub.order.domain.entity.StatusPedido;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN_LOJA','GERENTE','OPERADOR')")
@Tag(name = "Admin · Orders", description = "Gestão administrativa de pedidos e transições de status")
public class AdminOrderController {

    private final OrderService orderService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    @Operation(summary = "Listagem paginada de pedidos com filtros por status e busca textual no numero")
    public Page<PedidoResumoResponse> listar(
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) String q,
            Pageable pageable
    ) {
        return orderService.listarAdmin(status, q, pageable);
    }

    @GetMapping("/stats")
    @Operation(summary = "Contagem de pedidos por status (para dashboard)")
    public Map<StatusPedido, Long> stats() {
        return orderService.statsByStatus();
    }

    @GetMapping("/{numeroPedido}")
    @Operation(summary = "Detalhe completo do pedido (sem checagem de owner)")
    public PedidoResponse detalhe(@PathVariable String numeroPedido) {
        return orderService.detalheAdmin(numeroPedido);
    }

    @PostMapping("/{numeroPedido}/transition")
    @PreAuthorize("hasAnyRole('ADMIN_LOJA','GERENTE')")
    @Operation(summary = "Transição manual de status (admin) — valida regras da OrderStateMachine")
    public PedidoResponse transicionar(@PathVariable String numeroPedido,
                                       @Valid @RequestBody TransicaoRequest req) {
        var current = currentUserProvider.requireCurrent();
        var pedido = orderService.transicionarByNumero(numeroPedido, req.paraStatus(), AtorTipo.ADMIN,
                current.id(), req.observacao());
        return orderService.toResponse(pedido);
    }
}
