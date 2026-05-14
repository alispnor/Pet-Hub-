package com.alispnor.pethub.order.application.usecase;

import com.alispnor.pethub.common.exception.BusinessRuleException;
import com.alispnor.pethub.common.exception.ForbiddenException;
import com.alispnor.pethub.common.exception.ResourceNotFoundException;
import com.alispnor.pethub.identity.infrastructure.persistence.UsuarioRepository;
import com.alispnor.pethub.inventory.application.usecase.InventoryService;
import com.alispnor.pethub.order.application.dto.CriarPedidoRequest;
import com.alispnor.pethub.order.application.dto.PedidoItemResponse;
import com.alispnor.pethub.order.application.dto.PedidoResponse;
import com.alispnor.pethub.order.application.dto.PedidoResumoResponse;
import com.alispnor.pethub.order.domain.OrderStateMachine;
import com.alispnor.pethub.order.domain.entity.AtorTipo;
import com.alispnor.pethub.order.domain.entity.Pedido;
import com.alispnor.pethub.order.domain.entity.PedidoEvento;
import com.alispnor.pethub.order.domain.entity.PedidoItem;
import com.alispnor.pethub.order.domain.entity.StatusPedido;
import com.alispnor.pethub.order.domain.entity.TipoEvento;
import com.alispnor.pethub.order.infrastructure.persistence.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final OrderNumberGenerator orderNumberGenerator;
    private final InventoryService inventoryService;

    // ---------------------------------------------------------------- Criação

    /**
     * Materializa o pedido em status PENDENTE_PAGAMENTO. As reservas de estoque
     * já devem ter sido feitas pelo caller (checkout) e podem ser confirmadas
     * pelo próximo transition para PAGAMENTO_APROVADO.
     */
    @Transactional
    public Pedido criar(CriarPedidoRequest req) {
        log.info("Order criar enter — cliente={}, itens={}, total={}",
                req.clienteId(), req.itens().size(), req.valorTotal());

        if (req.itens() == null || req.itens().isEmpty()) {
            throw new BusinessRuleException("Pedido não pode ser criado sem itens");
        }

        var cliente = usuarioRepository.findById(req.clienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente " + req.clienteId() + " não encontrado"));

        var numero = orderNumberGenerator.next();

        var pedido = Pedido.builder()
                .numeroPedido(numero)
                .cliente(cliente)
                .status(StatusPedido.PENDENTE_PAGAMENTO)
                .enderecoEntregaSnapshot(req.enderecoEntregaSnapshot())
                .enderecoCobrancaSnapshot(req.enderecoCobrancaSnapshot())
                .opcaoFreteSnapshot(req.opcaoFreteSnapshot())
                .cupomCodigo(req.cupomCodigo())
                .valorSubtotal(req.valorSubtotal())
                .valorDescontos(req.valorDescontos())
                .valorImpostos(req.valorImpostos())
                .valorFrete(req.valorFrete())
                .valorTotal(req.valorTotal())
                .formaPagamentoTipo(req.formaPagamentoTipo())
                .formaPagamentoUltimos4(req.formaPagamentoUltimos4())
                .formaPagamentoBandeira(req.formaPagamentoBandeira())
                .build();

        for (var snap : req.itens()) {
            var item = PedidoItem.builder()
                    .produtoId(snap.produtoId())
                    .sku(snap.sku())
                    .nomeProduto(snap.nomeProduto())
                    .imagemUrl(snap.imagemUrl())
                    .qty(snap.qty())
                    .precoUnitario(snap.precoUnitario())
                    .descontoUnitario(snap.descontoUnitario())
                    .precoFinal(snap.precoFinal())
                    .ncm(snap.ncm())
                    .build();
            pedido.adicionarItem(item);
        }

        var evento = PedidoEvento.builder()
                .tipo(TipoEvento.PEDIDO_CRIADO)
                .descricao("Pedido criado pelo cliente")
                .ocorridoEm(LocalDateTime.now())
                .atorTipo(AtorTipo.CLIENTE)
                .atorId(req.clienteId())
                .build();
        pedido.adicionarEvento(evento);

        var saved = pedidoRepository.save(pedido);
        log.info("Order criar exit — numero={}, id={}", saved.getNumeroPedido(), saved.getId());
        return saved;
    }

    @Transactional
    public void vincularTentativaPagamento(Long pedidoId, Long tentativaPagamentoId) {
        log.info("vincularTentativaPagamento — pedidoId={}, tentativaId={}", pedidoId, tentativaPagamentoId);
        var pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido " + pedidoId + " não encontrado"));
        pedido.setTentativaPagamentoId(tentativaPagamentoId);
        pedidoRepository.save(pedido);
    }

    // ---------------------------------------------------------- Transições

    @Transactional
    public Pedido transicionar(Long pedidoId, StatusPedido novoStatus,
                               AtorTipo atorTipo, Long atorId, String observacao) {
        log.info("Transicionar enter — pedidoId={}, novoStatus={}, ator={}/{}",
                pedidoId, novoStatus, atorTipo, atorId);

        var pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido " + pedidoId + " não encontrado"));
        return transicionarInternal(pedido, novoStatus, atorTipo, atorId, observacao);
    }

    @Transactional
    public Pedido transicionarByNumero(String numeroPedido, StatusPedido novoStatus,
                                       AtorTipo atorTipo, Long atorId, String observacao) {
        log.info("Transicionar by numero enter — numero={}, novoStatus={}", numeroPedido, novoStatus);
        var pedido = pedidoRepository.findByNumeroPedido(numeroPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido " + numeroPedido + " não encontrado"));
        return transicionarInternal(pedido, novoStatus, atorTipo, atorId, observacao);
    }

    private Pedido transicionarInternal(Pedido pedido, StatusPedido novoStatus,
                                        AtorTipo atorTipo, Long atorId, String observacao) {
        var statusAnterior = pedido.getStatus();
        OrderStateMachine.requireTransicaoValida(statusAnterior, novoStatus);

        // Hooks ANTES de mudar o status no pedido
        dispatchHooks(pedido, statusAnterior, novoStatus);

        pedido.setStatus(novoStatus);

        var evento = PedidoEvento.builder()
                .tipo(eventoParaStatus(novoStatus))
                .descricao(observacao != null && !observacao.isBlank()
                        ? observacao
                        : "Transição " + statusAnterior + " → " + novoStatus)
                .ocorridoEm(LocalDateTime.now())
                .atorTipo(atorTipo)
                .atorId(atorId)
                .payload(payloadTransicao(statusAnterior, novoStatus))
                .build();
        pedido.adicionarEvento(evento);

        var saved = pedidoRepository.save(pedido);
        log.info("Transicionar exit — numero={}, {} → {}", saved.getNumeroPedido(), statusAnterior, novoStatus);
        return saved;
    }

    /** Dispara os efeitos colaterais associados a cada transição. */
    private void dispatchHooks(Pedido pedido, StatusPedido de, StatusPedido para) {
        var ref = pedido.getNumeroPedido();
        if (de == StatusPedido.PENDENTE_PAGAMENTO && para == StatusPedido.PAGAMENTO_APROVADO) {
            log.info("Hook: confirmando reservas de estoque para {}", ref);
            inventoryService.confirmarReservas(ref);
            return;
        }
        if (de == StatusPedido.PENDENTE_PAGAMENTO && para == StatusPedido.PAGAMENTO_REJEITADO) {
            log.info("Hook: liberando reservas (pagamento rejeitado) para {}", ref);
            inventoryService.liberarReservas(ref, false);
            return;
        }
        if (para == StatusPedido.CANCELADO) {
            if (de == StatusPedido.PENDENTE_PAGAMENTO) {
                log.info("Hook: liberando reservas (cancelamento pre-pagamento) para {}", ref);
                inventoryService.liberarReservas(ref, false);
            } else {
                // Cancelamento pós-aprovado/separação. Spec da Fase 4 trata o estorno
                // de estoque + pagamento como movimentação manual de admin — não
                // automatizamos aqui para não silenciar fluxo financeiro.
                log.warn("Cancelamento de {} a partir de {} requer estorno manual (estoque + pagamento)",
                        ref, de);
            }
        }
    }

    private TipoEvento eventoParaStatus(StatusPedido status) {
        return switch (status) {
            case PENDENTE_PAGAMENTO -> TipoEvento.PEDIDO_CRIADO;
            case PAGAMENTO_APROVADO -> TipoEvento.PAGAMENTO_APROVADO;
            case PAGAMENTO_REJEITADO -> TipoEvento.PAGAMENTO_REJEITADO;
            case SEPARACAO -> TipoEvento.SEPARACAO_INICIADA;
            case EM_TRANSPORTE -> TipoEvento.EM_TRANSPORTE;
            case ENTREGUE -> TipoEvento.ENTREGUE;
            case CANCELADO -> TipoEvento.CANCELADO;
            case DEVOLVIDO -> TipoEvento.DEVOLVIDO;
        };
    }

    private Map<String, Object> payloadTransicao(StatusPedido de, StatusPedido para) {
        var p = new HashMap<String, Object>();
        p.put("statusAnterior", de.name());
        p.put("statusNovo", para.name());
        return p;
    }

    /** Cancelamento iniciado pelo próprio cliente. Só permite a partir de PENDENTE_PAGAMENTO. */
    @Transactional
    public Pedido cancelarPeloCliente(String numeroPedido, Long clienteId) {
        log.info("Cancelar pelo cliente enter — numero={}, cliente={}", numeroPedido, clienteId);
        var pedido = pedidoRepository.findByNumeroPedido(numeroPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido " + numeroPedido + " não encontrado"));
        if (!pedido.getCliente().getId().equals(clienteId)) {
            throw new ForbiddenException("Pedido pertence a outro usuário");
        }
        if (pedido.getStatus() != StatusPedido.PENDENTE_PAGAMENTO) {
            throw new BusinessRuleException(
                    "Cancelamento pelo cliente só é permitido enquanto o pedido está PENDENTE_PAGAMENTO. "
                    + "Status atual: " + pedido.getStatus());
        }
        return transicionarInternal(pedido, StatusPedido.CANCELADO, AtorTipo.CLIENTE, clienteId,
                "Cancelado pelo cliente");
    }

    // ---------------------------------------------------------- Consultas

    @Transactional(readOnly = true)
    public Page<PedidoResumoResponse> listarDoCliente(Long clienteId, StatusPedido status,
                                                       LocalDateTime dataInicio, LocalDateTime dataFim,
                                                       Pageable pageable) {
        return pedidoRepository
                .searchByCliente(clienteId, status, dataInicio, dataFim, pageable)
                .map(this::toResumoResponse);
    }

    @Transactional(readOnly = true)
    public PedidoResponse detalheCliente(String numeroPedido, Long clienteId) {
        var pedido = pedidoRepository.findByNumeroPedido(numeroPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido " + numeroPedido + " não encontrado"));
        if (!pedido.getCliente().getId().equals(clienteId)) {
            throw new ForbiddenException("Pedido pertence a outro usuário");
        }
        return toResponse(pedido);
    }

    @Transactional(readOnly = true)
    public Pedido carregarParaTimeline(String numeroPedido, Long clienteId) {
        var pedido = pedidoRepository.findByNumeroPedido(numeroPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido " + numeroPedido + " não encontrado"));
        if (!pedido.getCliente().getId().equals(clienteId)) {
            throw new ForbiddenException("Pedido pertence a outro usuário");
        }
        // Força o load dos eventos enquanto a transação está aberta
        pedido.getEventos().size();
        return pedido;
    }

    @Transactional(readOnly = true)
    public Page<PedidoResumoResponse> listarAdmin(StatusPedido status, String q, Pageable pageable) {
        return pedidoRepository.searchAdmin(status, q, pageable).map(this::toResumoResponse);
    }

    @Transactional(readOnly = true)
    public PedidoResponse detalheAdmin(String numeroPedido) {
        var pedido = pedidoRepository.findByNumeroPedido(numeroPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido " + numeroPedido + " não encontrado"));
        return toResponse(pedido);
    }

    @Transactional(readOnly = true)
    public Map<StatusPedido, Long> statsByStatus() {
        var rows = pedidoRepository.countByStatus();
        var result = new java.util.EnumMap<StatusPedido, Long>(StatusPedido.class);
        for (var s : StatusPedido.values()) {
            result.put(s, 0L);
        }
        for (var row : rows) {
            result.put((StatusPedido) row[0], (Long) row[1]);
        }
        return result;
    }

    // ---------------------------------------------------------- Mappers

    public PedidoResponse toResponse(Pedido p) {
        return new PedidoResponse(
                p.getId(),
                p.getNumeroPedido(),
                p.getCliente().getId(),
                p.getStatus(),
                p.getItens().stream().map(this::toItemResponse).collect(Collectors.toList()),
                p.getEnderecoEntregaSnapshot(),
                p.getEnderecoCobrancaSnapshot(),
                p.getOpcaoFreteSnapshot(),
                p.getCupomCodigo(),
                p.getValorSubtotal(),
                p.getValorDescontos(),
                p.getValorImpostos(),
                p.getValorFrete(),
                p.getValorTotal(),
                p.getFormaPagamentoTipo(),
                p.getFormaPagamentoUltimos4(),
                p.getFormaPagamentoBandeira(),
                p.getTentativaPagamentoId(),
                p.getObservacoes(),
                p.getCriadoEm(),
                p.getAtualizadoEm()
        );
    }

    private PedidoItemResponse toItemResponse(PedidoItem i) {
        return new PedidoItemResponse(
                i.getId(),
                i.getProdutoId(),
                i.getSku(),
                i.getNomeProduto(),
                i.getImagemUrl(),
                i.getQty(),
                i.getPrecoUnitario(),
                i.getDescontoUnitario(),
                i.getPrecoFinal(),
                i.getNcm()
        );
    }

    private PedidoResumoResponse toResumoResponse(Pedido p) {
        var totalItens = p.getItens().stream().mapToInt(PedidoItem::getQty).sum();
        return new PedidoResumoResponse(
                p.getId(),
                p.getNumeroPedido(),
                p.getStatus(),
                p.getValorTotal(),
                p.getFormaPagamentoTipo(),
                totalItens,
                p.getCriadoEm()
        );
    }

    public List<PedidoEvento> eventos(Pedido p) {
        return p.getEventos();
    }
}
