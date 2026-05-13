package com.alispnor.pethub.pricing.domain.entity;

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

@Entity
@Table(name = "promocoes_itens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromocaoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promocao_id", nullable = false)
    private Promocao promocao;

    /**
     * Para CATEGORIA/PRODUTO usar a coluna id (FK lógica). Para MARCA usar
     * `referenciaValor` (slug textual). Mantemos as duas para não exigir uma
     * tabela de marcas só para um lookup simples nessa fase.
     */
    @Column(name = "referencia_id")
    private Long referenciaId;

    @Column(name = "referencia_valor", length = 100)
    private String referenciaValor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_referencia", nullable = false, length = 20)
    private TipoReferencia tipoReferencia;
}
