package com.alispnor.pethub.customer.domain.entity;

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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "formas_pagamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"perfilCliente", "gatewayToken"})
public class FormaPagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "perfil_cliente_id", nullable = false)
    private PerfilCliente perfilCliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoPagamento tipo;

    @Column(length = 50)
    private String apelido;

    @Column(name = "gateway_token", length = 100)
    private String gatewayToken;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Bandeira bandeira;

    @Column(name = "ultimos_quatro_digitos", length = 4)
    private String ultimosQuatroDigitos;

    @Column(name = "nome_impresso", length = 100)
    private String nomeImpresso;

    @Column(name = "validade_mes")
    private Integer validadeMes;

    @Column(name = "validade_ano")
    private Integer validadeAno;

    @Column(nullable = false)
    @Builder.Default
    private boolean padrao = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean ativo = true;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;
}
