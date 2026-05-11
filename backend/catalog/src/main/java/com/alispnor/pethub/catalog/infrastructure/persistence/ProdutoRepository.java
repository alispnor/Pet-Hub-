package com.alispnor.pethub.catalog.infrastructure.persistence;

import com.alispnor.pethub.catalog.domain.entity.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    Optional<Produto> findBySku(String sku);

    Optional<Produto> findBySkuAndAtivoTrue(String sku);

    boolean existsBySku(String sku);

    Page<Produto> findByAtivoTrue(Pageable pageable);

    Page<Produto> findByAtivoTrueAndCategoria_Slug(String categoriaSlug, Pageable pageable);

    @Query("""
            SELECT p FROM Produto p
            WHERE p.ativo = true
              AND (LOWER(p.nome) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(p.descricaoCurta) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(p.marca) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Produto> search(@Param("q") String query, Pageable pageable);
}
