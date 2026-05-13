package com.alispnor.pethub.checkout.application.usecase;

import com.alispnor.pethub.cart.application.usecase.CartService;
import com.alispnor.pethub.cart.domain.CartItem;
import com.alispnor.pethub.cart.infrastructure.persistence.CartRepository;
import com.alispnor.pethub.checkout.application.dto.CheckoutPreviewRequest;
import com.alispnor.pethub.checkout.application.dto.CheckoutPreviewResponse;
import com.alispnor.pethub.common.exception.BusinessRuleException;
import com.alispnor.pethub.common.exception.ForbiddenException;
import com.alispnor.pethub.common.exception.ResourceNotFoundException;
import com.alispnor.pethub.customer.domain.entity.Endereco;
import com.alispnor.pethub.customer.infrastructure.persistence.EnderecoRepository;
import com.alispnor.pethub.pricing.application.usecase.PriceCalculatorService;
import com.alispnor.pethub.shipping.application.dto.OpcaoFreteResponse;
import com.alispnor.pethub.shipping.application.dto.ShippingCalculateRequest;
import com.alispnor.pethub.shipping.application.dto.ShippingItemRequest;
import com.alispnor.pethub.shipping.application.usecase.ShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final CartRepository cartRepository;
    private final CartService cartService;
    private final EnderecoRepository enderecoRepository;
    private final ShippingService shippingService;
    private final PriceCalculatorService priceCalculatorService;

    @Transactional(readOnly = true)
    public CheckoutPreviewResponse preview(Long usuarioId, CheckoutPreviewRequest request) {
        log.info("Checkout preview enter — user={}, enderecoId={}, freteCodigo={}, cupomRequest={}",
                usuarioId, request.enderecoEntregaId(), request.opcaoFreteCodigo(), request.cupom());

        var cart = cartRepository.findByUserId(usuarioId)
                .orElseThrow(() -> new BusinessRuleException("Carrinho vazio"));
        if (cart.items().isEmpty()) {
            throw new BusinessRuleException("Carrinho vazio");
        }

        var endereco = loadOwnedEndereco(usuarioId, request.enderecoEntregaId());
        if (!endereco.isAtivo()) {
            throw new BusinessRuleException("Endereço de entrega inativo");
        }

        var opcoes = shippingService.calcular(toShippingRequest(endereco.getCep(), cart.items()));
        var freteEscolhido = opcoes.stream()
                .filter(o -> o.codigo().equalsIgnoreCase(request.opcaoFreteCodigo()))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(
                        "Opção de frete " + request.opcaoFreteCodigo() + " não está disponível para este destino"));

        var cupomCodigo = request.cupom() != null && !request.cupom().isBlank()
                ? request.cupom().trim().toUpperCase()
                : (cart.cupom() == null ? null : cart.cupom().codigo());

        var breakdown = priceCalculatorService.calcular(
                cart.items(),
                cupomCodigo,
                endereco.getUf().name(),
                freteEscolhido.valor(),
                usuarioId
        );

        var response = new CheckoutPreviewResponse(
                breakdown.itens(),
                breakdown.subtotal(),
                breakdown.descontoPromocoes(),
                breakdown.descontoCupom(),
                breakdown.impostos(),
                freteEscolhido,
                breakdown.valorTotal(),
                breakdown.cupomCodigo()
        );
        log.info("Checkout preview exit — user={}, subtotal={}, descontos={}+{}, frete={}, total={}",
                usuarioId, breakdown.subtotal(), breakdown.descontoPromocoes(), breakdown.descontoCupom(),
                freteEscolhido.valor(), breakdown.valorTotal());
        return response;
    }

    Endereco loadOwnedEndereco(Long usuarioId, Long enderecoId) {
        var endereco = enderecoRepository.findById(enderecoId)
                .orElseThrow(() -> new ResourceNotFoundException("Endereço " + enderecoId + " não encontrado"));
        var ownerUserId = endereco.getPerfilCliente().getUsuario().getId();
        if (!ownerUserId.equals(usuarioId)) {
            log.warn("Checkout: usuário {} tentou usar endereço {} (dono é {})",
                    usuarioId, enderecoId, ownerUserId);
            throw new ForbiddenException("Endereço pertence a outro usuário");
        }
        return endereco;
    }

    private ShippingCalculateRequest toShippingRequest(String cep, List<CartItem> items) {
        var sis = items.stream()
                .map(i -> new ShippingItemRequest(i.sku(), i.qty()))
                .toList();
        return new ShippingCalculateRequest(cep, sis);
    }
}
