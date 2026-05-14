package com.alispnor.pethub.order.application.usecase;

import com.alispnor.pethub.order.application.dto.EtapaTimeline;
import com.alispnor.pethub.order.application.dto.TimelineResponse;
import com.alispnor.pethub.order.domain.entity.Pedido;
import com.alispnor.pethub.order.domain.entity.PedidoEvento;
import com.alispnor.pethub.order.domain.entity.StatusPedido;
import com.alispnor.pethub.order.domain.entity.TipoEvento;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calcula a timeline visual para o cliente. Cada etapa mostra status (CONCLUIDA
 * / ATUAL / PENDENTE) e tempo decorrido entre ela e a anterior.
 */
@Slf4j
@Service
public class TimelineService {

    private static final List<StatusPedido> ETAPAS_FELIZES = List.of(
            StatusPedido.PENDENTE_PAGAMENTO,
            StatusPedido.PAGAMENTO_APROVADO,
            StatusPedido.SEPARACAO,
            StatusPedido.EM_TRANSPORTE,
            StatusPedido.ENTREGUE
    );

    private static final Map<StatusPedido, String> NOMES = Map.of(
            StatusPedido.PENDENTE_PAGAMENTO,  "Pedido recebido",
            StatusPedido.PAGAMENTO_APROVADO,  "Pagamento aprovado",
            StatusPedido.SEPARACAO,           "Em separação",
            StatusPedido.EM_TRANSPORTE,       "Em transporte",
            StatusPedido.ENTREGUE,            "Entregue",
            StatusPedido.PAGAMENTO_REJEITADO, "Pagamento rejeitado",
            StatusPedido.CANCELADO,           "Cancelado",
            StatusPedido.DEVOLVIDO,           "Devolvido"
    );

    public TimelineResponse construir(Pedido pedido) {
        log.debug("TimelineService.construir — numero={}", pedido.getNumeroPedido());
        var statusAtual = pedido.getStatus();
        var ocorridoPorStatus = mapearOcorrencias(pedido.getEventos());
        var etapas = new ArrayList<EtapaTimeline>();

        // Caminhos terminais não-felizes: retornar com etapa final dedicada
        if (statusAtual == StatusPedido.PAGAMENTO_REJEITADO
                || statusAtual == StatusPedido.CANCELADO
                || statusAtual == StatusPedido.DEVOLVIDO) {
            etapas.add(etapaFelizConcluida(StatusPedido.PENDENTE_PAGAMENTO, ocorridoPorStatus, pedido.getCriadoEm()));
            var ocorridoAtual = ocorridoPorStatus.getOrDefault(statusAtual, pedido.getAtualizadoEm());
            var anterior = pedido.getCriadoEm();
            etapas.add(new EtapaTimeline(
                    NOMES.get(statusAtual),
                    "ATUAL",
                    ocorridoAtual,
                    tempoDecorrido(anterior, ocorridoAtual),
                    null
            ));
            return new TimelineResponse(pedido.getNumeroPedido(), statusAtual, etapas);
        }

        var idxAtual = ETAPAS_FELIZES.indexOf(statusAtual);
        LocalDateTime anterior = null;
        for (int i = 0; i < ETAPAS_FELIZES.size(); i++) {
            var etapa = ETAPAS_FELIZES.get(i);
            var ocorridoEm = ocorridoPorStatus.get(etapa);
            String estado;
            if (i < idxAtual) {
                estado = "CONCLUIDA";
            } else if (i == idxAtual) {
                estado = "ATUAL";
            } else {
                estado = "PENDENTE";
            }
            var tempo = (ocorridoEm == null || anterior == null) ? null : tempoDecorrido(anterior, ocorridoEm);
            etapas.add(new EtapaTimeline(NOMES.get(etapa), estado, ocorridoEm, tempo, null));
            if (ocorridoEm != null) {
                anterior = ocorridoEm;
            }
        }
        return new TimelineResponse(pedido.getNumeroPedido(), statusAtual, etapas);
    }

    private EtapaTimeline etapaFelizConcluida(StatusPedido s, Map<StatusPedido, LocalDateTime> mapa,
                                              LocalDateTime fallback) {
        return new EtapaTimeline(
                NOMES.get(s),
                "CONCLUIDA",
                mapa.getOrDefault(s, fallback),
                "instantâneo",
                null
        );
    }

    private Map<StatusPedido, LocalDateTime> mapearOcorrencias(List<PedidoEvento> eventos) {
        var mapa = new LinkedHashMap<StatusPedido, LocalDateTime>();
        for (var ev : eventos) {
            var status = statusByEvento(ev.getTipo());
            if (status != null) {
                // mantém o primeiro registro (mais antigo) para a etapa
                mapa.putIfAbsent(status, ev.getOcorridoEm());
            }
        }
        return mapa;
    }

    private StatusPedido statusByEvento(TipoEvento tipo) {
        return switch (tipo) {
            case PEDIDO_CRIADO -> StatusPedido.PENDENTE_PAGAMENTO;
            case PAGAMENTO_APROVADO -> StatusPedido.PAGAMENTO_APROVADO;
            case PAGAMENTO_REJEITADO -> StatusPedido.PAGAMENTO_REJEITADO;
            case SEPARACAO_INICIADA -> StatusPedido.SEPARACAO;
            case EM_TRANSPORTE -> StatusPedido.EM_TRANSPORTE;
            case ENTREGUE -> StatusPedido.ENTREGUE;
            case CANCELADO -> StatusPedido.CANCELADO;
            case DEVOLVIDO -> StatusPedido.DEVOLVIDO;
            case OBSERVACAO_ADMIN -> null;
        };
    }

    private String tempoDecorrido(LocalDateTime de, LocalDateTime ate) {
        if (de == null || ate == null) {
            return null;
        }
        var dur = Duration.between(de, ate);
        if (dur.toMinutes() < 1) {
            return "instantâneo";
        }
        var dias = dur.toDays();
        var horas = dur.minusDays(dias).toHours();
        var mins = dur.minusDays(dias).minusHours(horas).toMinutes();
        if (dias > 0) {
            return dias + (dias == 1 ? " dia " : " dias ") + horas + "h";
        }
        if (horas > 0) {
            return horas + "h " + mins + "min";
        }
        return mins + " minutos";
    }
}
