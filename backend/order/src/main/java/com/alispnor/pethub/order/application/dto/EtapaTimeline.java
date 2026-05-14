package com.alispnor.pethub.order.application.dto;

import java.time.LocalDateTime;

public record EtapaTimeline(
        String nome,
        String status,
        LocalDateTime ocorridoEm,
        String tempoDecorrido,
        String previsaoEntrega
) {
}
