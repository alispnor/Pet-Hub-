package com.alispnor.pethub.inventory.infrastructure.persistence;

import com.alispnor.pethub.inventory.domain.entity.Estoque;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EstoqueRepository extends JpaRepository<Estoque, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Estoque e where e.produto.id = :produtoId")
    Optional<Estoque> findByProdutoIdForUpdate(@Param("produtoId") Long produtoId);

    @Query("select e from Estoque e where e.produto.id = :produtoId")
    Optional<Estoque> findByProdutoId(@Param("produtoId") Long produtoId);

    @Query("select e from Estoque e where e.produto.sku = :sku")
    Optional<Estoque> findByProdutoSku(@Param("sku") String sku);

    @Query("""
        select e from Estoque e
        where (e.quantidade - e.quantidadeReservada) <= e.quantidadeMinima
        order by (e.quantidade - e.quantidadeReservada) asc
        """)
    Page<Estoque> findLowStock(Pageable pageable);
}
