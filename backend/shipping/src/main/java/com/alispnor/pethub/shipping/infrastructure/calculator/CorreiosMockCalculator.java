package com.alispnor.pethub.shipping.infrastructure.calculator;

import com.alispnor.pethub.shipping.application.dto.OpcaoFreteResponse;
import com.alispnor.pethub.shipping.application.dto.PackageData;
import com.alispnor.pethub.shipping.application.usecase.ShippingCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Placeholder do Correios. Na Fase 11+ vira integração real (Frenet/Melhor Envio).
 * Simula instabilidade: com probabilidade `app.shipping.correios-failure-rate`
 * (default 0.1) retorna lista vazia, fazendo o serviço cair no tabelado.
 */
@Slf4j
@Component
public class CorreiosMockCalculator implements ShippingCalculator {

    private final double failureRate;

    public CorreiosMockCalculator(@Value("${app.shipping.correios-failure-rate:0.1}") double failureRate) {
        this.failureRate = Math.min(Math.max(failureRate, 0.0), 1.0);
    }

    @Override
    public List<OpcaoFreteResponse> calcular(String cepDestino, PackageData pkg) {
        if (cepDestino == null || cepDestino.length() != 8) {
            return List.of();
        }
        if (ThreadLocalRandom.current().nextDouble() < failureRate) {
            log.warn("Correios MOCK indisponível para CEP {} (failureRate={})", cepDestino, failureRate);
            return List.of();
        }
        var pac = new BigDecimal("22.50")
                .add(new BigDecimal("1.30").multiply(pkg.pesoFaturavelKg()))
                .setScale(2, RoundingMode.HALF_UP);
        var sedex = pac.multiply(new BigDecimal("1.75")).setScale(2, RoundingMode.HALF_UP);

        log.info("Correios MOCK para CEP {}: PAC={} SEDEX={} peso={}kg",
                cepDestino, pac, sedex, pkg.pesoFaturavelKg());
        return List.of(
                new OpcaoFreteResponse("PAC", "Correios", "PAC", pac, 9),
                new OpcaoFreteResponse("SEDEX", "Correios", "SEDEX", sedex, 4)
        );
    }

    @Override
    public String name() {
        return "CORREIOS_MOCK";
    }
}
