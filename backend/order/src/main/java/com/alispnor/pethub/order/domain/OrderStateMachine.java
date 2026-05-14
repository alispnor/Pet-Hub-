package com.alispnor.pethub.order.domain;

import com.alispnor.pethub.common.exception.BusinessRuleException;
import com.alispnor.pethub.order.domain.entity.StatusPedido;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.alispnor.pethub.order.domain.entity.StatusPedido.CANCELADO;
import static com.alispnor.pethub.order.domain.entity.StatusPedido.DEVOLVIDO;
import static com.alispnor.pethub.order.domain.entity.StatusPedido.EM_TRANSPORTE;
import static com.alispnor.pethub.order.domain.entity.StatusPedido.ENTREGUE;
import static com.alispnor.pethub.order.domain.entity.StatusPedido.PAGAMENTO_APROVADO;
import static com.alispnor.pethub.order.domain.entity.StatusPedido.PAGAMENTO_REJEITADO;
import static com.alispnor.pethub.order.domain.entity.StatusPedido.PENDENTE_PAGAMENTO;
import static com.alispnor.pethub.order.domain.entity.StatusPedido.SEPARACAO;

/**
 * Define as transições válidas do ciclo de vida do pedido. Não persiste —
 * apenas valida. Hooks de efeito (confirmar reserva, baixar estoque) ficam
 * em {@code OrderService.transicionar()}.
 *
 * <p>Diagrama:
 * <pre>
 * PENDENTE_PAGAMENTO ──► PAGAMENTO_APROVADO ──► SEPARACAO ──► EM_TRANSPORTE ──► ENTREGUE
 *         │                                        │             │
 *         ├──► PAGAMENTO_REJEITADO (final)         │             │
 *         └──► CANCELADO                           └───► CANCELADO
 *                                                                ENTREGUE ──► DEVOLVIDO
 * </pre>
 */
public final class OrderStateMachine {

    private static final Map<StatusPedido, Set<StatusPedido>> TRANSICOES_VALIDAS = Map.of(
            PENDENTE_PAGAMENTO, EnumSet.of(PAGAMENTO_APROVADO, PAGAMENTO_REJEITADO, CANCELADO),
            PAGAMENTO_APROVADO, EnumSet.of(SEPARACAO, CANCELADO),
            SEPARACAO,          EnumSet.of(EM_TRANSPORTE, CANCELADO),
            EM_TRANSPORTE,      EnumSet.of(ENTREGUE),
            ENTREGUE,           EnumSet.of(DEVOLVIDO),
            PAGAMENTO_REJEITADO, EnumSet.noneOf(StatusPedido.class),
            CANCELADO,           EnumSet.noneOf(StatusPedido.class),
            DEVOLVIDO,           EnumSet.noneOf(StatusPedido.class)
    );

    private OrderStateMachine() {
    }

    public static boolean isTransicaoValida(StatusPedido origem, StatusPedido destino) {
        return TRANSICOES_VALIDAS.getOrDefault(origem, EnumSet.noneOf(StatusPedido.class)).contains(destino);
    }

    /**
     * @throws BusinessRuleException se a transição não for permitida.
     */
    public static void requireTransicaoValida(StatusPedido origem, StatusPedido destino) {
        if (!isTransicaoValida(origem, destino)) {
            throw new BusinessRuleException(
                    "Transição inválida: " + origem + " → " + destino
                    + ". Transições permitidas a partir de " + origem + ": "
                    + TRANSICOES_VALIDAS.get(origem));
        }
    }

    public static Set<StatusPedido> proximosPermitidos(StatusPedido origem) {
        return EnumSet.copyOf(TRANSICOES_VALIDAS.getOrDefault(origem, EnumSet.noneOf(StatusPedido.class)));
    }

    public static boolean isFinal(StatusPedido status) {
        return proximosPermitidos(status).isEmpty();
    }
}
