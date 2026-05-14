package com.alispnor.pethub.checkout.application.usecase;

import com.alispnor.pethub.cart.application.usecase.CartService;
import com.alispnor.pethub.cart.domain.CartItem;
import com.alispnor.pethub.cart.infrastructure.persistence.CartRepository;
import com.alispnor.pethub.checkout.application.dto.CheckoutPreviewRequest;
import com.alispnor.pethub.checkout.application.dto.CheckoutPreviewResponse;
import com.alispnor.pethub.checkout.application.dto.PlaceOrderRequest;
import com.alispnor.pethub.checkout.application.dto.PlaceOrderResponse;
import com.alispnor.pethub.checkout.domain.entity.TentativaPagamento;
import com.alispnor.pethub.checkout.infrastructure.persistence.TentativaPagamentoRepository;
import com.alispnor.pethub.common.exception.BusinessRuleException;
import com.alispnor.pethub.common.exception.ForbiddenException;
import com.alispnor.pethub.common.exception.ResourceNotFoundException;
import com.alispnor.pethub.customer.domain.entity.Endereco;
import com.alispnor.pethub.customer.domain.entity.FormaPagamento;
import com.alispnor.pethub.customer.domain.entity.TipoPagamento;
import com.alispnor.pethub.customer.infrastructure.persistence.EnderecoRepository;
import com.alispnor.pethub.customer.infrastructure.persistence.FormaPagamentoRepository;
import com.alispnor.pethub.identity.infrastructure.persistence.UsuarioRepository;
import com.alispnor.pethub.payment.domain.PaymentGateway;
import com.alispnor.pethub.pricing.application.usecase.PriceCalculatorService;
import com.alispnor.pethub.shipping.application.dto.OpcaoFreteResponse;
import com.alispnor.pethub.shipping.application.dto.ShippingCalculateRequest;
import com.alispnor.pethub.shipping.application.dto.ShippingItemRequest;
import com.alispnor.pethub.shipping.application.usecase.ShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private static final DateTimeFormatter REF_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final CartRepository cartRepository;
    private final CartService cartService;
    private final EnderecoRepository enderecoRepository;
    private final FormaPagamentoRepository formaPagamentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ShippingService shippingService;
    private final PriceCalculatorService priceCalculatorService;
    private final PaymentGateway paymentGateway;
    private final TentativaPagamentoRepository tentativaPagamentoRepository;

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

    @Transactional
    public PlaceOrderResponse placeOrder(Long usuarioId, PlaceOrderRequest request) {
        log.info("Place-order enter — user={}, enderecoEntrega={}, enderecoCobranca={}, formaPagamento={}, idem={}",
                usuarioId, request.enderecoEntregaId(), request.enderecoCobrancaId(),
                request.formaPagamentoId(), request.idempotencyKey());

        var idempotencyKey = request.idempotencyKey() == null || request.idempotencyKey().isBlank()
                ? UUID.randomUUID().toString()
                : request.idempotencyKey();

        var existente = tentativaPagamentoRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existente != null) {
            log.info("Place-order idempotente — devolvendo tentativa existente id={}", existente.getId());
            return toResponse(existente);
        }

        var preview = preview(usuarioId, new CheckoutPreviewRequest(
                request.enderecoEntregaId(),
                request.opcaoFreteCodigo(),
                request.cupom()
        ));

        loadOwnedEndereco(usuarioId, request.enderecoCobrancaId());
        var formaPagamento = loadOwnedFormaPagamento(usuarioId, request.formaPagamentoId());
        var metodo = mapMetodo(formaPagamento.getTipo());

        var referenciaPedido = "PH-" + LocalDateTime.now().format(REF_FORMATTER)
                + "-" + String.format("%04d", ThreadLocalRandom.current().nextInt(10_000));

        var chargeRequest = new PaymentGateway.ChargeRequest(
                referenciaPedido,
                preview.valorTotal(),
                metodo,
                formaPagamento.getGatewayToken(),
                request.parcelas(),
                idempotencyKey
        );
        var paymentResult = paymentGateway.charge(chargeRequest);

        var rawResponse = new HashMap<String, Object>();
        rawResponse.put("status", paymentResult.status().name());
        rawResponse.put("transactionId", paymentResult.transactionId());
        rawResponse.put("metodo", paymentResult.method().name());
        rawResponse.put("rawJson", paymentResult.rawResponse());

        var tentativa = TentativaPagamento.builder()
                .referenciaPedido(referenciaPedido)
                .usuario(usuarioRepository.getReferenceById(usuarioId))
                .formaPagamentoId(formaPagamento.getId())
                .metodo(metodo)
                .valor(preview.valorTotal())
                .gatewayTransactionId(paymentResult.transactionId())
                .status(paymentResult.status())
                .qrCode(paymentResult.qrCode())
                .boletoUrl(paymentResult.boletoUrl())
                .responseGateway(rawResponse)
                .idempotencyKey(idempotencyKey)
                .build();
        var saved = tentativaPagamentoRepository.save(tentativa);

        if (paymentResult.status() == PaymentGateway.Status.APPROVED) {
            log.info("Pagamento aprovado para pedido {} (tentativa {}). Limpando carrinho do user {}",
                    referenciaPedido, saved.getId(), usuarioId);
            cartService.limpar(usuarioId);
        } else {
            log.warn("Pagamento {} para pedido {} (tentativa {})",
                    paymentResult.status(), referenciaPedido, saved.getId());
        }

        var response = toResponse(saved);
        log.info("Place-order exit — pedido={}, status={}, txn={}",
                referenciaPedido, paymentResult.status(), paymentResult.transactionId());
        return response;
    }

    private FormaPagamento loadOwnedFormaPagamento(Long usuarioId, Long formaPagamentoId) {
        var forma = formaPagamentoRepository.findById(formaPagamentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Forma de pagamento " + formaPagamentoId + " não encontrada"));
        var ownerUserId = forma.getPerfilCliente().getUsuario().getId();
        if (!ownerUserId.equals(usuarioId)) {
            log.warn("Checkout: usuário {} tentou usar forma de pagamento {} (dono é {})",
                    usuarioId, formaPagamentoId, ownerUserId);
            throw new ForbiddenException("Forma de pagamento pertence a outro usuário");
        }
        if (!forma.isAtivo()) {
            throw new BusinessRuleException("Forma de pagamento inativa");
        }
        return forma;
    }

    private PaymentGateway.Method mapMetodo(TipoPagamento tipo) {
        return switch (tipo) {
            case CARTAO_CREDITO -> PaymentGateway.Method.CARTAO_CREDITO;
            case CARTAO_DEBITO -> PaymentGateway.Method.CARTAO_DEBITO;
            case PIX -> PaymentGateway.Method.PIX;
            case BOLETO -> PaymentGateway.Method.BOLETO;
        };
    }

    private PlaceOrderResponse toResponse(TentativaPagamento t) {
        return new PlaceOrderResponse(
                t.getId(),
                t.getReferenciaPedido(),
                t.getMetodo(),
                t.getStatus(),
                t.getValor(),
                t.getGatewayTransactionId(),
                t.getQrCode(),
                t.getBoletoUrl()
        );
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
