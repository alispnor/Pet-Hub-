package com.alispnor.pethub.order.application.usecase;

import com.alispnor.pethub.order.domain.entity.PedidoSequence;
import com.alispnor.pethub.order.infrastructure.persistence.PedidoSequenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Gera o número humano do pedido no formato {@code PH-YYYY-NNNNNN} usando uma
 * tabela {@code pedido_sequence} por ano com lock pessimista no incremento —
 * garante unicidade mesmo com N transações concorrentes.
 *
 * <p>Roda em transação própria ({@link Propagation#REQUIRES_NEW}) para que o lock
 * pessimista seja liberado o mais rápido possível, sem segurar a transação maior
 * que cria o Pedido.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNumberGenerator {

    private final PedidoSequenceRepository sequenceRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String next() {
        log.debug("OrderNumberGenerator.next() enter");
        var ano = LocalDate.now().getYear();
        var seq = sequenceRepository.findByAnoForUpdate(ano).orElseGet(() ->
                sequenceRepository.save(PedidoSequence.builder().ano(ano).ultimoNumero(0).build())
        );
        var proximo = seq.getUltimoNumero() + 1;
        seq.setUltimoNumero(proximo);
        sequenceRepository.save(seq);

        var numero = String.format("PH-%d-%06d", ano, proximo);
        log.debug("OrderNumberGenerator.next() exit — numero={}", numero);
        return numero;
    }
}
