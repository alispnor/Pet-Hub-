package com.alispnor.pethub.inventory.application.usecase;

import com.alispnor.pethub.catalog.infrastructure.persistence.ProdutoRepository;
import com.alispnor.pethub.common.exception.BusinessRuleException;
import com.alispnor.pethub.common.exception.ResourceNotFoundException;
import com.alispnor.pethub.identity.infrastructure.persistence.UsuarioRepository;
import com.alispnor.pethub.inventory.application.dto.EstoqueResponse;
import com.alispnor.pethub.inventory.application.dto.ItemReserva;
import com.alispnor.pethub.inventory.application.dto.MovimentacaoManualRequest;
import com.alispnor.pethub.inventory.application.dto.MovimentacaoResponse;
import com.alispnor.pethub.inventory.domain.entity.Estoque;
import com.alispnor.pethub.inventory.domain.entity.MovimentacaoEstoque;
import com.alispnor.pethub.inventory.domain.entity.ReservaEstoque;
import com.alispnor.pethub.inventory.domain.entity.StatusReserva;
import com.alispnor.pethub.inventory.domain.entity.TipoMovimentacao;
import com.alispnor.pethub.inventory.infrastructure.persistence.EstoqueRepository;
import com.alispnor.pethub.inventory.infrastructure.persistence.MovimentacaoEstoqueRepository;
import com.alispnor.pethub.inventory.infrastructure.persistence.ReservaEstoqueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Estoque com reservas. Toda mutação passa por lock pessimista para evitar
 * race conditions sob alta concorrência (último item da prateleira sendo
 * disputado por 10 threads — só 1 sucede).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    public static final int RESERVA_TTL_MINUTOS = 15;

    private final EstoqueRepository estoqueRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final ReservaEstoqueRepository reservaRepository;
    private final ProdutoRepository produtoRepository;
    private final UsuarioRepository usuarioRepository;

    // ---------------------------------------------------------------- Reservas

    /**
     * Reserva estoque por {@link #RESERVA_TTL_MINUTOS} minutos. Cria uma linha
     * em {@code reservas_estoque} por SKU e incrementa {@code quantidade_reservada}
     * no {@code estoques}. Se qualquer item falhar, a transação inteira aborta.
     */
    @Transactional
    public List<ReservaEstoque> reservar(String referenciaPedido, Long pedidoId, List<ItemReserva> itens) {
        log.info("Reservar enter — refPedido={}, pedidoId={}, itens={}", referenciaPedido, pedidoId, itens.size());

        validarReferencia(referenciaPedido);
        if (itens.isEmpty()) {
            throw new BusinessRuleException("Lista de itens para reserva está vazia");
        }

        // Ordena por SKU para evitar deadlock entre transações que reservam o mesmo conjunto em ordem diferente
        var itensOrdenados = itens.stream()
                .sorted((a, b) -> a.sku().compareTo(b.sku()))
                .toList();

        var expiraEm = LocalDateTime.now().plusMinutes(RESERVA_TTL_MINUTOS);
        var reservasCriadas = new ArrayList<ReservaEstoque>();

        for (var item : itensOrdenados) {
            var produto = produtoRepository.findBySku(item.sku())
                    .orElseThrow(() -> new ResourceNotFoundException("Produto SKU " + item.sku() + " não encontrado"));
            var estoque = estoqueRepository.findByProdutoIdForUpdate(produto.getId())
                    .orElseThrow(() -> new BusinessRuleException("Sem estoque cadastrado para SKU " + item.sku()));

            if (estoque.disponivel() < item.qty()) {
                throw new BusinessRuleException(
                        "Estoque insuficiente para SKU " + item.sku()
                        + " (disponível: " + estoque.disponivel() + ", solicitado: " + item.qty() + ")");
            }

            estoque.setQuantidadeReservada(estoque.getQuantidadeReservada() + item.qty());
            estoqueRepository.save(estoque);

            var reserva = ReservaEstoque.builder()
                    .produto(produto)
                    .pedidoId(pedidoId)
                    .referenciaPedido(referenciaPedido)
                    .quantidade(item.qty())
                    .status(StatusReserva.ATIVA)
                    .expiraEm(expiraEm)
                    .build();
            reservasCriadas.add(reservaRepository.save(reserva));

            registrarMovimentacao(produto.getId(), TipoMovimentacao.RESERVA, item.qty(),
                    "Reserva para pedido " + referenciaPedido, pedidoId, referenciaPedido, null);
        }

        log.info("Reservar exit — refPedido={}, reservas={}, expiraEm={}",
                referenciaPedido, reservasCriadas.size(), expiraEm);
        return reservasCriadas;
    }

    /**
     * Pagamento aprovado: confirma as reservas, decrementa {@code quantidade} e
     * {@code quantidade_reservada} (efetivando a baixa) e marca as reservas como
     * CONFIRMADAS.
     */
    @Transactional
    public void confirmarReservas(String referenciaPedido) {
        log.info("Confirmar reservas enter — refPedido={}", referenciaPedido);
        validarReferencia(referenciaPedido);

        var reservas = reservaRepository.findByReferenciaPedidoAndStatus(referenciaPedido, StatusReserva.ATIVA);
        if (reservas.isEmpty()) {
            log.warn("Nenhuma reserva ATIVA encontrada para refPedido={} (idempotência ou expirada)", referenciaPedido);
            return;
        }

        for (var reserva : reservas) {
            var produtoId = reserva.getProduto().getId();
            var estoque = estoqueRepository.findByProdutoIdForUpdate(produtoId)
                    .orElseThrow(() -> new BusinessRuleException(
                            "Estoque sumiu para produto " + produtoId + " (refPedido=" + referenciaPedido + ")"));
            estoque.setQuantidade(estoque.getQuantidade() - reserva.getQuantidade());
            estoque.setQuantidadeReservada(estoque.getQuantidadeReservada() - reserva.getQuantidade());
            estoqueRepository.save(estoque);

            reserva.setStatus(StatusReserva.CONFIRMADA);
            reservaRepository.save(reserva);

            registrarMovimentacao(produtoId, TipoMovimentacao.BAIXA_VENDA, reserva.getQuantidade(),
                    "Baixa por venda — pedido " + referenciaPedido,
                    reserva.getPedidoId(), referenciaPedido, null);
        }

        log.info("Confirmar reservas exit — refPedido={}, reservas confirmadas={}", referenciaPedido, reservas.size());
    }

    /**
     * Pagamento rejeitado, cancelado ou expirado: libera as reservas
     * (decrementa {@code quantidade_reservada}) e marca as reservas como
     * CANCELADAS (ou EXPIRADAS se {@code marcarExpirada}=true).
     */
    @Transactional
    public void liberarReservas(String referenciaPedido, boolean marcarExpirada) {
        log.info("Liberar reservas enter — refPedido={}, expirada={}", referenciaPedido, marcarExpirada);
        validarReferencia(referenciaPedido);

        var reservas = reservaRepository.findByReferenciaPedidoAndStatus(referenciaPedido, StatusReserva.ATIVA);
        if (reservas.isEmpty()) {
            log.info("Nenhuma reserva ATIVA para liberar — refPedido={}", referenciaPedido);
            return;
        }

        var statusFinal = marcarExpirada ? StatusReserva.EXPIRADA : StatusReserva.CANCELADA;
        for (var reserva : reservas) {
            var produtoId = reserva.getProduto().getId();
            var estoque = estoqueRepository.findByProdutoIdForUpdate(produtoId)
                    .orElseThrow(() -> new BusinessRuleException(
                            "Estoque sumiu para produto " + produtoId));
            estoque.setQuantidadeReservada(estoque.getQuantidadeReservada() - reserva.getQuantidade());
            estoqueRepository.save(estoque);

            reserva.setStatus(statusFinal);
            reservaRepository.save(reserva);

            registrarMovimentacao(produtoId, TipoMovimentacao.LIBERACAO_RESERVA, reserva.getQuantidade(),
                    "Liberação de reserva (" + statusFinal + ") — pedido " + referenciaPedido,
                    reserva.getPedidoId(), referenciaPedido, null);
        }

        log.info("Liberar reservas exit — refPedido={}, reservas liberadas={}, status={}",
                referenciaPedido, reservas.size(), statusFinal);
    }

    /**
     * Coleta reservas expiradas (ATIVA com {@code expira_em < agora}) e libera
     * cada uma. Chamado pelo {@link ReservaExpiracaoJob} a cada minuto.
     *
     * @return mapa {@code referencia_pedido → quantidade de reservas liberadas}.
     *         Pedidos referenciados precisam ser cancelados (responsabilidade do order-service).
     */
    @Transactional
    public Map<String, Integer> expirarReservasVencidas() {
        var agora = LocalDateTime.now();
        log.info("Expirar reservas vencidas enter — agora={}", agora);

        var expiradas = reservaRepository.findExpiradas(StatusReserva.ATIVA, agora);
        if (expiradas.isEmpty()) {
            log.info("Nenhuma reserva expirada");
            return Map.of();
        }

        // Agrupa por refPedido para liberar todas juntas (transação por refPedido).
        var porRef = new HashMap<String, Integer>();
        var refsUnicas = expiradas.stream().map(ReservaEstoque::getReferenciaPedido).distinct().toList();
        for (var ref : refsUnicas) {
            liberarReservas(ref, true);
            porRef.merge(ref, 1, Integer::sum);
        }

        log.info("Expirar reservas exit — pedidos afetados={}, total reservas={}",
                porRef.size(), expiradas.size());
        return porRef;
    }

    // -------------------------------------------------------------- Movimentação manual

    @Transactional
    public MovimentacaoResponse registrarMovimentacaoManual(Long adminUserId, MovimentacaoManualRequest req) {
        log.info("Movimentacao manual enter — admin={}, sku={}, tipo={}, qty={}",
                adminUserId, req.sku(), req.tipo(), req.quantidade());

        var produto = produtoRepository.findBySku(req.sku())
                .orElseThrow(() -> new ResourceNotFoundException("Produto SKU " + req.sku() + " não encontrado"));
        var estoque = estoqueRepository.findByProdutoIdForUpdate(produto.getId())
                .orElseThrow(() -> new BusinessRuleException("Sem estoque cadastrado para SKU " + req.sku()));

        switch (req.tipo()) {
            case ENTRADA, AJUSTE -> estoque.setQuantidade(estoque.getQuantidade() + req.quantidade());
            case SAIDA -> {
                if (estoque.disponivel() < req.quantidade()) {
                    throw new BusinessRuleException(
                            "Saída maior que disponível (disp=" + estoque.disponivel() + ", req=" + req.quantidade() + ")");
                }
                estoque.setQuantidade(estoque.getQuantidade() - req.quantidade());
            }
            default -> throw new BusinessRuleException(
                    "Movimentação manual só aceita ENTRADA/SAIDA/AJUSTE; tipo " + req.tipo() + " é interno");
        }
        estoqueRepository.save(estoque);

        var mov = registrarMovimentacao(produto.getId(), req.tipo(), req.quantidade(),
                req.motivo(), null, null, adminUserId);

        log.info("Movimentacao manual exit — id={}, novoSaldo={}", mov.getId(), estoque.getQuantidade());
        return toMovimentacaoResponse(mov);
    }

    // ----------------------------------------------------------- Consultas

    @Transactional(readOnly = true)
    public Page<EstoqueResponse> listar(Pageable pageable) {
        return estoqueRepository.findAll(pageable).map(this::toEstoqueResponse);
    }

    @Transactional(readOnly = true)
    public Page<EstoqueResponse> listarLowStock(Pageable pageable) {
        return estoqueRepository.findLowStock(pageable).map(this::toEstoqueResponse);
    }

    @Transactional(readOnly = true)
    public Page<MovimentacaoResponse> historicoBySku(String sku, Pageable pageable) {
        return movimentacaoRepository.findByProdutoSkuOrderByCriadoEmDesc(sku, pageable)
                .map(this::toMovimentacaoResponse);
    }

    @Transactional(readOnly = true)
    public EstoqueResponse detalheBySku(String sku) {
        var estoque = estoqueRepository.findByProdutoSku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Sem estoque cadastrado para SKU " + sku));
        return toEstoqueResponse(estoque);
    }

    // ----------------------------------------------------------- Helpers

    private MovimentacaoEstoque registrarMovimentacao(
            Long produtoId, TipoMovimentacao tipo, int quantidade, String motivo,
            Long pedidoId, String referenciaPedido, Long criadoPorId) {

        var mov = MovimentacaoEstoque.builder()
                .produto(produtoRepository.getReferenceById(produtoId))
                .tipo(tipo)
                .quantidade(quantidade)
                .motivo(motivo)
                .pedidoId(pedidoId)
                .referenciaPedido(referenciaPedido)
                .criadoPor(criadoPorId == null ? null : usuarioRepository.getReferenceById(criadoPorId))
                .build();
        return movimentacaoRepository.save(mov);
    }

    private void validarReferencia(String referenciaPedido) {
        if (referenciaPedido == null || referenciaPedido.isBlank() || referenciaPedido.length() > 50) {
            throw new BusinessRuleException("Referência de pedido inválida: " + referenciaPedido);
        }
    }

    private EstoqueResponse toEstoqueResponse(Estoque e) {
        return new EstoqueResponse(
                e.getId(),
                e.getProduto().getId(),
                e.getProduto().getSku(),
                e.getProduto().getNome(),
                e.getQuantidade(),
                e.getQuantidadeReservada(),
                e.disponivel(),
                e.getQuantidadeMinima(),
                e.getLocalizacao()
        );
    }

    private MovimentacaoResponse toMovimentacaoResponse(MovimentacaoEstoque m) {
        return new MovimentacaoResponse(
                m.getId(),
                m.getProduto().getSku(),
                m.getTipo(),
                m.getQuantidade(),
                m.getMotivo(),
                m.getPedidoId(),
                m.getReferenciaPedido(),
                m.getCriadoPor() == null ? null : m.getCriadoPor().getId(),
                m.getCriadoEm()
        );
    }
}
