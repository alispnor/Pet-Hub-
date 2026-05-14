package com.alispnor.pethub.order.infrastructure.rest;

import com.alispnor.pethub.identity.infrastructure.security.CurrentUserProvider;
import com.alispnor.pethub.order.application.dto.PedidoResponse;
import com.alispnor.pethub.order.application.dto.PedidoResumoResponse;
import com.alispnor.pethub.order.application.dto.TimelineResponse;
import com.alispnor.pethub.order.application.usecase.OrderService;
import com.alispnor.pethub.order.application.usecase.TimelineService;
import com.alispnor.pethub.order.domain.entity.StatusPedido;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Pedidos do cliente — listagem, detalhe, timeline e cancelamento")
public class OrderController {

    private final OrderService orderService;
    private final TimelineService timelineService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    @Operation(summary = "Histórico paginado de pedidos do cliente autenticado (com filtros opcionais)")
    public Page<PedidoResumoResponse> listar(
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim,
            Pageable pageable
    ) {
        var current = currentUserProvider.requireCurrent();
        return orderService.listarDoCliente(current.id(), status, dataInicio, dataFim, pageable);
    }

    @GetMapping("/{numeroPedido}")
    @Operation(summary = "Detalhe completo do pedido pelo número humano (PH-YYYY-NNNNNN)")
    public PedidoResponse detalhe(@PathVariable String numeroPedido) {
        var current = currentUserProvider.requireCurrent();
        return orderService.detalheCliente(numeroPedido, current.id());
    }

    @GetMapping("/{numeroPedido}/timeline")
    @Operation(summary = "Timeline visual do pedido com etapas (CONCLUIDA/ATUAL/PENDENTE) e tempo decorrido")
    public TimelineResponse timeline(@PathVariable String numeroPedido) {
        var current = currentUserProvider.requireCurrent();
        var pedido = orderService.carregarParaTimeline(numeroPedido, current.id());
        return timelineService.construir(pedido);
    }

    @PostMapping("/{numeroPedido}/cancel")
    @Operation(summary = "Cliente cancela o próprio pedido (apenas enquanto PENDENTE_PAGAMENTO)")
    public PedidoResponse cancelar(@PathVariable String numeroPedido) {
        var current = currentUserProvider.requireCurrent();
        orderService.cancelarPeloCliente(numeroPedido, current.id());
        return orderService.detalheCliente(numeroPedido, current.id());
    }
}
