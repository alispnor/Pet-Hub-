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
import com.alispnor.pethub.inventory.application.dto.ItemReserva;
import com.alispnor.pethub.inventory.application.usecase.InventoryService;
import com.alispnor.pethub.inventory.infrastructure.persistence.EstoqueRepository;
import com.alispnor.pethub.order.application.dto.CriarPedidoRequest;
import com.alispnor.pethub.order.application.dto.PedidoItemSnapshot;
import com.alispnor.pethub.order.application.usecase.OrderService;
import com.alispnor.pethub.order.domain.entity.AtorTipo;
import com.alispnor.pethub.order.domain.entity.Pedido;
import com.alispnor.pethub.order.domain.entity.StatusPedido;
import com.alispnor.pethub.payment.domain.PaymentGateway;
import com.alispnor.pethub.pricing.application.dto.PricingBreakdown;
import com.alispnor.pethub.pricing.application.usecase.PriceCalculatorService;
import com.alispnor.pethub.shipping.application.dto.OpcaoFreteResponse;
import com.alispnor.pethub.shipping.application.dto.ShippingCalculateRequest;
import com.alispnor.pethub.shipping.application.dto.ShippingItemRequest;
import com.alispnor.pethub.shipping.application.usecase.ShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final CartRepository cartRepository;
    private final CartService cartService;
    private final EnderecoRepository enderecoRepository;
    private final FormaPagamentoRepository formaPagamentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ShippingService shippingService;
    private final PriceCalculatorService priceCalculatorService;
    private final PaymentGateway paymentGateway;
    private final TentativaPagamentoRepository tentativaPagamentoRepository;
    private final InventoryService inventoryService;
    private final EstoqueRepository estoqueRepository;
    private final OrderService orderService;

    // -------------------------------------------------------------- Preview

    @Transactional(readOnly = true)
    public CheckoutPreviewResponse preview(Long usuarioId, CheckoutPreviewRequest request) {
        log.info("Checkout preview enter — user={}, enderecoId={}, freteCodigo={}, cupomRequest={}",
                usuarioId, request.enderecoEntregaId(), request.opcaoFreteCodigo(), request.cupom());

        var cart = cartRepository.findByUserId(usuarioId)
                .orElseThrow(() -> new BusinessRuleException("Carrinho vazio"));
        if (cart.items().isEmpty()) {
            throw new BusinessRuleException("Carrinho vazio");
        }

        validarEstoqueDisponivel(cart.items());

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

    // -------------------------------------------------------------- Place order

    /**
     * Confirma o pedido. Fluxo idempotente:
     * <ol>
     *   <li>se {@code idempotency_key} já existe → devolve a tentativa anterior</li>
     *   <li>reusa preview para revalidar tudo (estoque, endereço, frete, cupom)</li>
     *   <li>checa ownership de cobrança e forma de pagamento</li>
     *   <li>cria reservas de estoque (15min TTL via {@link InventoryService})</li>
     *   <li>materializa o pedido (status PENDENTE_PAGAMENTO)</li>
     *   <li>cobra via gateway</li>
     *   <li>persiste {@link TentativaPagamento} e vincula ao pedido</li>
     *   <li>transition para PAGAMENTO_APROVADO ou PAGAMENTO_REJEITADO (hooks confirmam/liberam reservas)</li>
     *   <li>limpa o carrinho no APROVADO</li>
     * </ol>
     */
    @Transactional
    public PlaceOrderResponse placeOrder(Long usuarioId, PlaceOrderRequest request) {
        log.info("Place-order enter — user={}, enderecoEntrega={}, enderecoCobranca={}, formaPagamento={}, idem={}",
                usuarioId, request.enderecoEntregaId(), request.enderecoCobrancaId(),
                request.formaPagamentoId(), request.idempotencyKey());

        var idempotencyKey = (request.idempotencyKey() == null || request.idempotencyKey().isBlank())
                ? UUID.randomUUID().toString()
                : request.idempotencyKey();

        var existente = tentativaPagamentoRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existente != null) {
            log.info("Place-order idempotente — devolvendo tentativa existente id={}", existente.getId());
            return toResponse(existente);
        }

        // 1. Revalidação completa via preview (que já checa estoque)
        var preview = preview(usuarioId, new CheckoutPreviewRequest(
                request.enderecoEntregaId(),
                request.opcaoFreteCodigo(),
                request.cupom()
        ));

        // 2. Ownership de cobrança + forma de pagamento
        var enderecoEntrega = loadOwnedEndereco(usuarioId, request.enderecoEntregaId());
        var enderecoCobranca = loadOwnedEndereco(usuarioId, request.enderecoCobrancaId());
        var formaPagamento = loadOwnedFormaPagamento(usuarioId, request.formaPagamentoId());
        var metodo = mapMetodo(formaPagamento.getTipo());

        // 3. Carrinho ainda existe nesse ponto (preview leu)
        var cart = cartRepository.findByUserId(usuarioId)
                .orElseThrow(() -> new BusinessRuleException("Carrinho vazio"));

        // 4. Cria pedido (PENDENTE_PAGAMENTO) — gerador de número usa REQUIRES_NEW
        var criarReq = new CriarPedidoRequest(
                usuarioId,
                toSnapshots(cart.items(), preview.itens()),
                snapshotEndereco(enderecoEntrega),
                snapshotEndereco(enderecoCobranca),
                snapshotFrete(preview.freteEscolhido()),
                preview.cupomCodigo(),
                preview.subtotal(),
                preview.descontoCupom().add(preview.descontoPromocoes()),
                preview.impostosCalculados() == null ? BigDecimal.ZERO : preview.impostosCalculados().total(),
                preview.freteEscolhido().valor(),
                preview.valorTotal(),
                metodo.name(),
                formaPagamento.getUltimosQuatroDigitos(),
                formaPagamento.getBandeira() == null ? null : formaPagamento.getBandeira().name()
        );
        var pedido = orderService.criar(criarReq);

        // 5. Reserva estoque para os itens do pedido (15min)
        var itensReserva = cart.items().stream()
                .map(i -> new ItemReserva(i.sku(), i.qty()))
                .toList();
        inventoryService.reservar(pedido.getNumeroPedido(), pedido.getId(), itensReserva);

        // 6. Cobra
        var chargeRequest = new PaymentGateway.ChargeRequest(
                pedido.getNumeroPedido(),
                preview.valorTotal(),
                metodo,
                formaPagamento.getGatewayToken(),
                request.parcelas(),
                idempotencyKey
        );
        var paymentResult = paymentGateway.charge(chargeRequest);

        // 7. Persiste TentativaPagamento (auditoria de cobrança)
        var rawResponse = new HashMap<String, Object>();
        rawResponse.put("status", paymentResult.status().name());
        rawResponse.put("transactionId", paymentResult.transactionId());
        rawResponse.put("metodo", paymentResult.method().name());
        rawResponse.put("rawJson", paymentResult.rawResponse());

        var tentativa = TentativaPagamento.builder()
                .referenciaPedido(pedido.getNumeroPedido())
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

        orderService.vincularTentativaPagamento(pedido.getId(), saved.getId());

        // 8. Transition do pedido conforme resultado da cobrança (hooks confirmam/liberam reservas)
        if (paymentResult.status() == PaymentGateway.Status.APPROVED) {
            orderService.transicionar(pedido.getId(), StatusPedido.PAGAMENTO_APROVADO,
                    AtorTipo.GATEWAY, null, "Pagamento aprovado pelo gateway");
            log.info("Pedido {} aprovado — limpando carrinho do user {}", pedido.getNumeroPedido(), usuarioId);
            cartService.limpar(usuarioId);
        } else if (paymentResult.status() == PaymentGateway.Status.REJECTED) {
            orderService.transicionar(pedido.getId(), StatusPedido.PAGAMENTO_REJEITADO,
                    AtorTipo.GATEWAY, null, "Pagamento rejeitado pelo gateway");
            log.warn("Pedido {} REJEITADO pelo gateway", pedido.getNumeroPedido());
        } else {
            // PROCESSING (PIX/BOLETO assíncrono): mantém PENDENTE_PAGAMENTO; transition virá via webhook.
            log.info("Pedido {} aguardando confirmação do gateway (status={})",
                    pedido.getNumeroPedido(), paymentResult.status());
        }

        var response = toResponse(saved, pedido);
        log.info("Place-order exit — pedido={}, status={}, txn={}",
                pedido.getNumeroPedido(), paymentResult.status(), paymentResult.transactionId());
        return response;
    }

    // --------------------------------------------------- Helpers de validação / mapper

    private void validarEstoqueDisponivel(List<CartItem> items) {
        var faltantes = new ArrayList<String>();
        for (var item : items) {
            var estoque = estoqueRepository.findByProdutoSku(item.sku()).orElse(null);
            if (estoque == null || estoque.disponivel() < item.qty()) {
                int disp = estoque == null ? 0 : estoque.disponivel();
                faltantes.add(item.sku() + " (disponível=" + disp + ", solicitado=" + item.qty() + ")");
            }
        }
        if (!faltantes.isEmpty()) {
            throw new BusinessRuleException("Estoque insuficiente para: " + String.join(", ", faltantes));
        }
    }

    private List<PedidoItemSnapshot> toSnapshots(List<CartItem> cartItems, List<CartItem> previewItens) {
        // Usa os itens do cart (têm precoUnitario, qty, nome, imagem). previewItens vem do PricingBreakdown
        // mas o conteúdo é o mesmo CartItem ajustado pelo pricing.
        var ref = previewItens != null && !previewItens.isEmpty() ? previewItens : cartItems;
        var out = new ArrayList<PedidoItemSnapshot>(ref.size());
        for (var i : ref) {
            var precoFinal = i.subtotal() == null ? i.precoUnitario().multiply(BigDecimal.valueOf(i.qty())) : i.subtotal();
            var desconto = i.precoUnitario().multiply(BigDecimal.valueOf(i.qty())).subtract(precoFinal);
            var descontoUnitario = i.qty() > 0
                    ? desconto.divide(BigDecimal.valueOf(i.qty()), 2, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            out.add(new PedidoItemSnapshot(
                    i.produtoId(),
                    i.sku(),
                    i.nome(),
                    i.imagemUrl(),
                    i.qty(),
                    i.precoUnitario(),
                    descontoUnitario.max(BigDecimal.ZERO),
                    precoFinal,
                    null  // NCM lookup futuro — não estritamente necessário aqui
            ));
        }
        return out;
    }

    private Map<String, Object> snapshotEndereco(Endereco e) {
        var m = new LinkedHashMap<String, Object>();
        m.put("id", e.getId());
        m.put("apelido", e.getApelido());
        m.put("cep", e.getCep());
        m.put("logradouro", e.getLogradouro());
        m.put("numero", e.getNumero());
        m.put("complemento", e.getComplemento());
        m.put("bairro", e.getBairro());
        m.put("cidade", e.getCidade());
        m.put("uf", e.getUf().name());
        m.put("pais", e.getPais());
        m.put("tipo", e.getTipo().name());
        return m;
    }

    private Map<String, Object> snapshotFrete(OpcaoFreteResponse f) {
        var m = new LinkedHashMap<String, Object>();
        m.put("codigo", f.codigo());
        m.put("transportadora", f.transportadora());
        m.put("servico", f.servico());
        m.put("valor", f.valor());
        m.put("prazoDias", f.prazoDias());
        return m;
    }

    private FormaPagamento loadOwnedFormaPagamento(Long usuarioId, Long formaPagamentoId) {
        var forma = formaPagamentoRepository.findById(formaPagamentoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Forma de pagamento " + formaPagamentoId + " não encontrada"));
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
                null,
                t.getMetodo(),
                t.getStatus(),
                t.getValor(),
                t.getGatewayTransactionId(),
                t.getQrCode(),
                t.getBoletoUrl()
        );
    }

    private PlaceOrderResponse toResponse(TentativaPagamento t, Pedido pedido) {
        return new PlaceOrderResponse(
                t.getId(),
                t.getReferenciaPedido(),
                pedido == null ? null : pedido.getId(),
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
