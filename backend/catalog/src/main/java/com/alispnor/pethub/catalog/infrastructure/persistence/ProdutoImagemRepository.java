package com.alispnor.pethub.catalog.infrastructure.persistence;

import com.alispnor.pethub.catalog.domain.entity.ProdutoImagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProdutoImagemRepository extends JpaRepository<ProdutoImagem, Long> {
}
