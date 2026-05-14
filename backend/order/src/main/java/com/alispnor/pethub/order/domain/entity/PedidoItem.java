package com.alispnor.pethub.order.domain.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "pedido_itens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "pedido")
public class PedidoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Column(name = "produto_id", nullable = false)
    private Long produtoId;

    @Column(nullable = false, length = 50)
    private String sku;

    @Column(name = "nome_produto", nullable = false, length = 200)
    private String nomeProduto;

    @Column(name = "imagem_url", length = 500)
    private String imagemUrl;

    @Column(nullable = false)
    private Integer qty;

    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    @Column(name = "desconto_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal descontoUnitario;

    @Column(name = "preco_final", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoFinal;

    @Column(length = 10)
    private String ncm;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> impostos;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;
}
