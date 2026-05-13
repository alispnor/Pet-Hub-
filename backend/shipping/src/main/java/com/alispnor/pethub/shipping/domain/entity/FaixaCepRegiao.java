package com.alispnor.pethub.shipping.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "faixas_cep_regiao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaixaCepRegiao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cep_inicio", nullable = false, length = 8)
    private String cepInicio;

    @Column(name = "cep_fim", nullable = false, length = 8)
    private String cepFim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Regiao regiao;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;
}
