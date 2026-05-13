package com.alispnor.pethub.shipping.application.usecase;

import com.alispnor.pethub.catalog.domain.entity.Produto;
import com.alispnor.pethub.catalog.infrastructure.persistence.ProdutoRepository;
import com.alispnor.pethub.common.exception.BusinessRuleException;
import com.alispnor.pethub.common.exception.ResourceNotFoundException;
import com.alispnor.pethub.shipping.application.dto.OpcaoFreteResponse;
import com.alispnor.pethub.shipping.application.dto.OpcoesFreteResponse;
import com.alispnor.pethub.shipping.application.dto.PackageData;
import com.alispnor.pethub.shipping.application.dto.ShippingCalculateRequest;
import com.alispnor.pethub.shipping.application.dto.ShippingItemRequest;
import com.alispnor.pethub.shipping.infrastructure.calculator.CorreiosMockCalculator;
import com.alispnor.pethub.shipping.infrastructure.calculator.MotoboyLocalCalculator;
import com.alispnor.pethub.shipping.infrastructure.calculator.TabeladoCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingService {

    private static final BigDecimal DENSIDADE_AEREA = new BigDecimal("6000"); // cm³/kg (peso cúbico)

    private final ProdutoRepository produtoRepository;
    private final CorreiosMockCalculator correios;
    private final MotoboyLocalCalculator motoboy;
    private final TabeladoCalculator tabelado;

    public List<OpcaoFreteResponse> calcular(ShippingCalculateRequest request) {
        return calcularCached(request).opcoes();
    }

    @Cacheable(value = "frete", key = "#request.cepDestino() + ':' + #request.itens().hashCode()")
    public OpcoesFreteResponse calcularCached(ShippingCalculateRequest request) {
        log.info("Calculando frete para CEP {} com {} itens", request.cepDestino(), request.itens().size());
        var pacote = montarPacote(request.itens());

        var opcoes = new ArrayList<OpcaoFreteResponse>();
        var correiosResult = correios.calcular(request.cepDestino(), pacote);
        opcoes.addAll(correiosResult);
        if (correiosResult.isEmpty()) {
            log.warn("Correios sem resposta para CEP {} — caindo para tabelado", request.cepDestino());
            opcoes.addAll(tabelado.calcular(request.cepDestino(), pacote));
        }
        opcoes.addAll(motoboy.calcular(request.cepDestino(), pacote));

        if (opcoes.isEmpty()) {
            throw new BusinessRuleException("Nenhuma opção de frete disponível para o CEP " + request.cepDestino());
        }
        opcoes.sort(Comparator.comparing(OpcaoFreteResponse::valor));
        log.info("Frete CEP {} resolvido com {} opções (menor R$ {}, peso faturável {}kg)",
                request.cepDestino(), opcoes.size(), opcoes.get(0).valor(), pacote.pesoFaturavelKg());
        return new OpcoesFreteResponse(List.copyOf(opcoes));
    }

    private PackageData montarPacote(List<ShippingItemRequest> itens) {
        var skus = itens.stream().map(ShippingItemRequest::sku).toList();
        var produtos = produtoRepository.findAll().stream()
                .filter(p -> skus.contains(p.getSku()))
                .toList();
        if (produtos.size() != skus.stream().distinct().count()) {
            throw new ResourceNotFoundException("Um ou mais SKUs não encontrados");
        }

        var pesoTotal = BigDecimal.ZERO;
        var volumeTotalCm3 = BigDecimal.ZERO;
        var qtdItens = 0;
        for (var item : itens) {
            var produto = produtos.stream()
                    .filter(p -> p.getSku().equals(item.sku()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("SKU " + item.sku() + " não encontrado"));
            pesoTotal = pesoTotal.add(produto.getPesoKg().multiply(BigDecimal.valueOf(item.qty())));
            volumeTotalCm3 = volumeTotalCm3.add(volumeCm3(produto).multiply(BigDecimal.valueOf(item.qty())));
            qtdItens += item.qty();
        }
        var pesoCubico = volumeTotalCm3.divide(DENSIDADE_AEREA, 3, RoundingMode.HALF_UP);
        var pesoFaturavel = pesoTotal.max(pesoCubico).setScale(3, RoundingMode.HALF_UP);
        return new PackageData(pesoTotal.setScale(3, RoundingMode.HALF_UP), pesoCubico, pesoFaturavel, qtdItens);
    }

    private BigDecimal volumeCm3(Produto produto) {
        var altura = produto.getAlturaCm();
        var largura = produto.getLarguraCm();
        var prof = produto.getProfundidadeCm();
        if (altura == null || largura == null || prof == null) {
            return BigDecimal.ZERO;
        }
        return altura.multiply(largura).multiply(prof);
    }
}
