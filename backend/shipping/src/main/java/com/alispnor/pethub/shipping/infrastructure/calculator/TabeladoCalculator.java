package com.alispnor.pethub.shipping.infrastructure.calculator;

import com.alispnor.pethub.shipping.application.dto.OpcaoFreteResponse;
import com.alispnor.pethub.shipping.application.dto.PackageData;
import com.alispnor.pethub.shipping.application.usecase.ShippingCalculator;
import com.alispnor.pethub.shipping.domain.entity.Regiao;
import com.alispnor.pethub.shipping.infrastructure.persistence.FaixaCepRegiaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Fallback tabelado: descobre a região via FaixaCepRegiao, soma um valor base
 * por região + R$ 1,80 por kg faturável. Prazo: depende da região.
 * Sempre retorna 2 opções (Econômico/Expresso) para dar variedade na UI.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TabeladoCalculator implements ShippingCalculator {

    private static final Map<Regiao, BigDecimal> BASE_POR_REGIAO = new EnumMap<>(Regiao.class);
    private static final Map<Regiao, int[]> PRAZO_DIAS = new EnumMap<>(Regiao.class);
    private static final BigDecimal POR_KG = new BigDecimal("1.80");

    static {
        BASE_POR_REGIAO.put(Regiao.SUDESTE, new BigDecimal("19.90"));
        BASE_POR_REGIAO.put(Regiao.SUL, new BigDecimal("24.90"));
        BASE_POR_REGIAO.put(Regiao.CENTRO_OESTE, new BigDecimal("29.90"));
        BASE_POR_REGIAO.put(Regiao.NORDESTE, new BigDecimal("34.90"));
        BASE_POR_REGIAO.put(Regiao.NORTE, new BigDecimal("39.90"));
        PRAZO_DIAS.put(Regiao.SUDESTE, new int[]{3, 6});
        PRAZO_DIAS.put(Regiao.SUL, new int[]{5, 9});
        PRAZO_DIAS.put(Regiao.CENTRO_OESTE, new int[]{6, 10});
        PRAZO_DIAS.put(Regiao.NORDESTE, new int[]{7, 12});
        PRAZO_DIAS.put(Regiao.NORTE, new int[]{10, 15});
    }

    private final FaixaCepRegiaoRepository faixaRepo;

    @Override
    public List<OpcaoFreteResponse> calcular(String cepDestino, PackageData pkg) {
        if (cepDestino == null || cepDestino.length() != 8) {
            return List.of();
        }
        var faixa = faixaRepo.findByCep(cepDestino).orElse(null);
        if (faixa == null) {
            log.warn("Tabelado sem faixa para CEP {}", cepDestino);
            return List.of();
        }
        var regiao = faixa.getRegiao();
        var base = BASE_POR_REGIAO.get(regiao);
        var prazo = PRAZO_DIAS.get(regiao);

        var valorEconomico = base.add(POR_KG.multiply(pkg.pesoFaturavelKg()))
                .setScale(2, RoundingMode.HALF_UP);
        var valorExpresso = valorEconomico.multiply(new BigDecimal("1.6"))
                .setScale(2, RoundingMode.HALF_UP);

        log.info("Tabelado para CEP {} ({}): econ={} expr={} peso={}kg",
                cepDestino, regiao, valorEconomico, valorExpresso, pkg.pesoFaturavelKg());
        return List.of(
                new OpcaoFreteResponse("TAB_ECON", "Pet Hub Tabelado", "Econômico", valorEconomico, prazo[1]),
                new OpcaoFreteResponse("TAB_EXPR", "Pet Hub Tabelado", "Expresso", valorExpresso, prazo[0])
        );
    }

    @Override
    public String name() {
        return "TABELADO";
    }
}
