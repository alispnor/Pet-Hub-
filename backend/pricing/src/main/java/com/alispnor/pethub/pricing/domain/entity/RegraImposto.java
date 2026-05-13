package com.alispnor.pethub.pricing.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "regras_imposto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegraImposto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 8)
    private String ncm;

    @Column(name = "uf_origem", nullable = false, length = 2)
    private String ufOrigem;

    @Column(name = "uf_destino", nullable = false, length = 2)
    private String ufDestino;

    @Column(name = "icms_aliquota", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal icmsAliquota = BigDecimal.ZERO;

    @Column(name = "ipi_aliquota", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal ipiAliquota = BigDecimal.ZERO;

    @Column(name = "pis_aliquota", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal pisAliquota = BigDecimal.ZERO;

    @Column(name = "cofins_aliquota", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal cofinsAliquota = BigDecimal.ZERO;

    @Column(name = "vigencia_inicio", nullable = false)
    private LocalDate vigenciaInicio;

    @Column(name = "vigencia_fim")
    private LocalDate vigenciaFim;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;
}
