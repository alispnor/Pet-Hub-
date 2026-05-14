package com.alispnor.pethub.checkout.domain.entity;

import com.alispnor.pethub.identity.domain.entity.Usuario;
import com.alispnor.pethub.payment.domain.PaymentGateway;
import io.hypersistence.utils.hibernate.type.json.JsonType;
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
import java.util.Map;

@Entity
@Table(name = "tentativas_pagamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"usuario", "responseGateway"})
public class TentativaPagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "referencia_pedido", nullable = false, length = 50)
    private String referenciaPedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "forma_pagamento_id")
    private Long formaPagamentoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentGateway.Method metodo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "gateway_transaction_id", length = 100)
    private String gatewayTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentGateway.Status status;

    @Column(name = "qr_code", columnDefinition = "text")
    private String qrCode;

    @Column(name = "boleto_url", length = 500)
    private String boletoUrl;

    @Type(JsonType.class)
    @Column(name = "response_gateway", columnDefinition = "jsonb")
    private Map<String, Object> responseGateway;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;
}
