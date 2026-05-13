package com.alispnor.pethub.shipping.application.usecase;

import com.alispnor.pethub.shipping.application.dto.OpcaoFreteResponse;
import com.alispnor.pethub.shipping.application.dto.PackageData;

import java.util.List;

public interface ShippingCalculator {

    /**
     * @return zero, uma ou várias opções de frete para o destino. Implementações
     *         que não atendem a esse CEP devem retornar lista vazia (não exceção).
     */
    List<OpcaoFreteResponse> calcular(String cepDestino, PackageData pkg);

    /** Identifica a implementação para logs e seleção de fallback. */
    String name();
}
