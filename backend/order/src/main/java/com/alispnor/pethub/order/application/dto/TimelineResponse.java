package com.alispnor.pethub.order.application.dto;

import com.alispnor.pethub.order.domain.entity.StatusPedido;

import java.util.List;

public record TimelineResponse(
        String numeroPedido,
        StatusPedido statusAtual,
        List<EtapaTimeline> etapas
) {
}
