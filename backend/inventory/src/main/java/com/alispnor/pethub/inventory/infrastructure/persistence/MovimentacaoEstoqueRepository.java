package com.alispnor.pethub.inventory.infrastructure.persistence;

import com.alispnor.pethub.inventory.domain.entity.MovimentacaoEstoque;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Long> {

    Page<MovimentacaoEstoque> findByProdutoSkuOrderByCriadoEmDesc(String sku, Pageable pageable);
}
