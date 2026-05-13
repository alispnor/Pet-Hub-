package com.alispnor.pethub.shipping.infrastructure.calculator;

import com.alispnor.pethub.shipping.application.dto.OpcaoFreteResponse;
import com.alispnor.pethub.shipping.application.dto.PackageData;
import com.alispnor.pethub.shipping.application.usecase.ShippingCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Motoboy local em São Paulo capital: CEPs entre 01000000 e 05999999 → R$ 15
 * fixo, 1 dia útil. Em outras regiões retorna lista vazia (sem opção).
 */
@Slf4j
@Component
public class MotoboyLocalCalculator implements ShippingCalculator {

    private static final String SP_CAPITAL_INICIO = "01000000";
    private static final String SP_CAPITAL_FIM = "05999999";
    private static final BigDecimal VALOR_FIXO = new BigDecimal("15.00");

    @Override
    public List<OpcaoFreteResponse> calcular(String cepDestino, PackageData pkg) {
        if (cepDestino == null || cepDestino.length() != 8) {
            return List.of();
        }
        if (cepDestino.compareTo(SP_CAPITAL_INICIO) < 0 || cepDestino.compareTo(SP_CAPITAL_FIM) > 0) {
            return List.of();
        }
        log.info("Motoboy local aplicável para CEP {} (peso={}kg)", cepDestino, pkg.pesoFaturavelKg());
        return List.of(new OpcaoFreteResponse(
                "MOTOBOY",
                "Pet Hub Motoboy",
                "Entrega no mesmo dia útil",
                VALOR_FIXO,
                1
        ));
    }

    @Override
    public String name() {
        return "MOTOBOY_LOCAL";
    }
}
