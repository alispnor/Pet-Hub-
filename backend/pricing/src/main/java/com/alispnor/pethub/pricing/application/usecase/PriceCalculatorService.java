package com.alispnor.pethub.pricing.application.usecase;

import com.alispnor.pethub.cart.domain.CartItem;
import com.alispnor.pethub.catalog.domain.entity.Produto;
import com.alispnor.pethub.catalog.infrastructure.persistence.ProdutoRepository;
import com.alispnor.pethub.common.exception.BusinessRuleException;
import com.alispnor.pethub.common.exception.ResourceNotFoundException;
import com.alispnor.pethub.pricing.application.dto.ImpostosCalculados;
import com.alispnor.pethub.pricing.application.dto.PricingBreakdown;
import com.alispnor.pethub.pricing.domain.entity.Cupom;
import com.alispnor.pethub.pricing.domain.entity.Promocao;
import com.alispnor.pethub.pricing.domain.entity.PromocaoItem;
import com.alispnor.pethub.pricing.domain.entity.TipoCupom;
import com.alispnor.pethub.pricing.domain.entity.TipoReferencia;
import com.alispnor.pethub.pricing.infrastructure.persistence.CupomRepository;
import com.alispnor.pethub.pricing.infrastructure.persistence.CupomUsoRepository;
import com.alispnor.pethub.pricing.infrastructure.persistence.PromocaoItemRepository;
import com.alispnor.pethub.pricing.infrastructure.persistence.PromocaoRepository;
import com.alispnor.pethub.pricing.infrastructure.persistence.RegraImpostoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceCalculatorService {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);
    private static final int SCALE = 2;

    private final CupomRepository cupomRepository;
    private final CupomUsoRepository cupomUsoRepository;
    private final PromocaoRepository promocaoRepository;
    private final PromocaoItemRepository promocaoItemRepository;
    private final RegraImpostoRepository regraImpostoRepository;
    private final ProdutoRepository produtoRepository;

    @Value("${app.shop.uf-origem:SP}")
    private String ufOrigemPadrao;

    @Transactional(readOnly = true)
    public PricingBreakdown calcular(List<CartItem> itens,
                                     String cupomCodigo,
                                     String ufDestino,
                                     BigDecimal valorFrete,
                                     Long usuarioId) {
        log.info("Calculando preços — itens={}, cupom={}, ufDestino={}, frete={}",
                itens.size(), cupomCodigo, ufDestino, valorFrete);

        if (itens.isEmpty()) {
            throw new BusinessRuleException("Carrinho vazio");
        }
        if (valorFrete == null) valorFrete = BigDecimal.ZERO;

        var produtos = carregarProdutos(itens);
        var subtotal = somar(itens.stream().map(CartItem::subtotal).toList());

        var promocoesVigentes = promocaoRepository.findVigentes(LocalDateTime.now());
        var descontoPromocoes = calcularDescontoPromocoes(itens, produtos, promocoesVigentes);

        var subtotalPosPromo = subtotal.subtract(descontoPromocoes).max(BigDecimal.ZERO);
        var descontoCupom = calcularDescontoCupom(cupomCodigo, subtotalPosPromo, usuarioId);

        var subtotalPosCupom = subtotalPosPromo.subtract(descontoCupom).max(BigDecimal.ZERO);
        var impostos = ufDestino == null ? ImpostosCalculados.zero()
                : calcularImpostos(itens, produtos, ufDestino);

        var valorTotal = subtotalPosCupom.add(valorFrete).setScale(SCALE, RoundingMode.HALF_UP);

        log.info("Preços calculados — subtotal={}, descPromos={}, descCupom={}, impostosTotal={}, frete={}, total={}",
                subtotal, descontoPromocoes, descontoCupom, impostos.total(), valorFrete, valorTotal);

        return new PricingBreakdown(
                itens,
                subtotal.setScale(SCALE, RoundingMode.HALF_UP),
                descontoPromocoes.setScale(SCALE, RoundingMode.HALF_UP),
                descontoCupom.setScale(SCALE, RoundingMode.HALF_UP),
                impostos,
                valorFrete.setScale(SCALE, RoundingMode.HALF_UP),
                valorTotal,
                cupomCodigo
        );
    }

    private Map<Long, Produto> carregarProdutos(List<CartItem> itens) {
        var ids = itens.stream().map(CartItem::produtoId).toList();
        var produtos = produtoRepository.findAllById(ids);
        var map = new HashMap<Long, Produto>(produtos.size());
        for (var p : produtos) {
            map.put(p.getId(), p);
        }
        for (var item : itens) {
            if (!map.containsKey(item.produtoId())) {
                throw new ResourceNotFoundException("Produto " + item.sku() + " não encontrado");
            }
        }
        return map;
    }

    private BigDecimal calcularDescontoPromocoes(List<CartItem> itens, Map<Long, Produto> produtos,
                                                 List<Promocao> promocoesVigentes) {
        if (promocoesVigentes.isEmpty()) return BigDecimal.ZERO;

        var itensPromocoes = new HashMap<Long, List<PromocaoItem>>();
        for (var promo : promocoesVigentes) {
            itensPromocoes.put(promo.getId(), promocaoItemRepository.findByPromocaoId(promo.getId()));
        }

        var total = BigDecimal.ZERO;
        for (var item : itens) {
            var produto = produtos.get(item.produtoId());
            var melhorPercentual = BigDecimal.ZERO;
            for (var promo : promocoesVigentes) {
                var alvos = itensPromocoes.get(promo.getId());
                if (matchPromocao(produto, alvos)) {
                    if (promo.getDescontoPercentual().compareTo(melhorPercentual) > 0) {
                        melhorPercentual = promo.getDescontoPercentual();
                    }
                }
            }
            if (melhorPercentual.signum() > 0) {
                var desconto = item.subtotal().multiply(melhorPercentual).divide(CEM, SCALE, RoundingMode.HALF_UP);
                total = total.add(desconto);
            }
        }
        return total;
    }

    private boolean matchPromocao(Produto produto, List<PromocaoItem> alvos) {
        for (var alvo : alvos) {
            if (alvo.getTipoReferencia() == TipoReferencia.PRODUTO
                    && produto.getId().equals(alvo.getReferenciaId())) {
                return true;
            }
            if (alvo.getTipoReferencia() == TipoReferencia.CATEGORIA
                    && produto.getCategoria() != null
                    && produto.getCategoria().getId().equals(alvo.getReferenciaId())) {
                return true;
            }
            if (alvo.getTipoReferencia() == TipoReferencia.MARCA
                    && produto.getMarca() != null
                    && produto.getMarca().equalsIgnoreCase(alvo.getReferenciaValor())) {
                return true;
            }
        }
        return false;
    }

    private BigDecimal calcularDescontoCupom(String codigo, BigDecimal subtotalAtual, Long usuarioId) {
        if (codigo == null || codigo.isBlank()) return BigDecimal.ZERO;

        var cupom = cupomRepository.findByCodigoIgnoreCase(codigo)
                .orElseThrow(() -> new BusinessRuleException("Cupom " + codigo + " inválido"));
        if (!cupom.estaVigente(LocalDateTime.now())) {
            throw new BusinessRuleException("Cupom " + codigo + " expirado ou inativo");
        }
        if (cupom.getValorMinimoCompra() != null
                && subtotalAtual.compareTo(cupom.getValorMinimoCompra()) < 0) {
            throw new BusinessRuleException("Cupom " + codigo + " exige compra mínima de "
                    + cupom.getValorMinimoCompra());
        }
        if (cupom.getUsoMaximoTotal() != null) {
            var totalUsos = cupomUsoRepository.countByCupomId(cupom.getId());
            if (totalUsos >= cupom.getUsoMaximoTotal()) {
                throw new BusinessRuleException("Cupom " + codigo + " atingiu o limite total de usos");
            }
        }
        if (cupom.getUsoMaximoPorCliente() != null && usuarioId != null) {
            var usosPorCliente = cupomUsoRepository.countByCupomIdAndUsuarioId(cupom.getId(), usuarioId);
            if (usosPorCliente >= cupom.getUsoMaximoPorCliente()) {
                throw new BusinessRuleException("Cupom " + codigo + " já atingiu o limite por cliente");
            }
        }

        return descontoDoCupom(cupom, subtotalAtual);
    }

    private BigDecimal descontoDoCupom(Cupom cupom, BigDecimal subtotalAtual) {
        if (cupom.getTipo() == TipoCupom.PERCENTUAL) {
            return subtotalAtual.multiply(cupom.getValor()).divide(CEM, SCALE, RoundingMode.HALF_UP);
        }
        return cupom.getValor().min(subtotalAtual);
    }

    private ImpostosCalculados calcularImpostos(List<CartItem> itens, Map<Long, Produto> produtos, String ufDestino) {
        var icms = BigDecimal.ZERO;
        var ipi = BigDecimal.ZERO;
        var pis = BigDecimal.ZERO;
        var cofins = BigDecimal.ZERO;
        var hoje = LocalDate.now();

        for (var item : itens) {
            var produto = produtos.get(item.produtoId());
            var ncm = produto.getNcm();
            if (ncm == null) continue;
            var regraOpt = regraImpostoRepository.findVigente(ncm, ufOrigemPadrao, ufDestino, hoje);
            if (regraOpt.isEmpty()) continue;
            var regra = regraOpt.get();
            var base = item.subtotal();
            icms = icms.add(base.multiply(regra.getIcmsAliquota()).divide(CEM, SCALE, RoundingMode.HALF_UP));
            ipi = ipi.add(base.multiply(regra.getIpiAliquota()).divide(CEM, SCALE, RoundingMode.HALF_UP));
            pis = pis.add(base.multiply(regra.getPisAliquota()).divide(CEM, SCALE, RoundingMode.HALF_UP));
            cofins = cofins.add(base.multiply(regra.getCofinsAliquota()).divide(CEM, SCALE, RoundingMode.HALF_UP));
        }
        var total = icms.add(ipi).add(pis).add(cofins);
        return new ImpostosCalculados(
                icms.setScale(SCALE, RoundingMode.HALF_UP),
                ipi.setScale(SCALE, RoundingMode.HALF_UP),
                pis.setScale(SCALE, RoundingMode.HALF_UP),
                cofins.setScale(SCALE, RoundingMode.HALF_UP),
                total.setScale(SCALE, RoundingMode.HALF_UP)
        );
    }

    private BigDecimal somar(List<BigDecimal> valores) {
        return valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
