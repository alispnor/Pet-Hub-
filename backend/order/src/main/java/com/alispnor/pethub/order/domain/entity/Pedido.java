package com.alispnor.pethub.order.domain.entity;

import com.alispnor.pethub.identity.domain.entity.Usuario;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "pedidos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"cliente", "itens", "eventos",
        "enderecoEntregaSnapshot", "enderecoCobrancaSnapshot", "opcaoFreteSnapshot"})
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_pedido", nullable = false, unique = true, length = 20)
    private String numeroPedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Usuario cliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusPedido status;

    @Type(JsonType.class)
    @Column(name = "endereco_entrega_snapshot", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> enderecoEntregaSnapshot;

    @Type(JsonType.class)
    @Column(name = "endereco_cobranca_snapshot", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> enderecoCobrancaSnapshot;

    @Type(JsonType.class)
    @Column(name = "opcao_frete_snapshot", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> opcaoFreteSnapshot;

    @Column(name = "cupom_codigo", length = 50)
    private String cupomCodigo;

    @Column(name = "valor_subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorSubtotal;

    @Column(name = "valor_descontos", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorDescontos;

    @Column(name = "valor_impostos", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorImpostos;

    @Column(name = "valor_frete", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorFrete;

    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "forma_pagamento_tipo", nullable = false, length = 20)
    private String formaPagamentoTipo;

    @Column(name = "forma_pagamento_ultimos4", length = 4)
    private String formaPagamentoUltimos4;

    @Column(name = "forma_pagamento_bandeira", length = 20)
    private String formaPagamentoBandeira;

    @Column(name = "tentativa_pagamento_id")
    private Long tentativaPagamentoId;

    @Column(length = 500)
    private String observacoes;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    @Builder.Default
    private List<PedidoItem> itens = new ArrayList<>();

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("ocorridoEm ASC, id ASC")
    @Builder.Default
    private List<PedidoEvento> eventos = new ArrayList<>();

    public void adicionarItem(PedidoItem item) {
        item.setPedido(this);
        this.itens.add(item);
    }

    public void adicionarEvento(PedidoEvento evento) {
        evento.setPedido(this);
        this.eventos.add(evento);
    }
}
