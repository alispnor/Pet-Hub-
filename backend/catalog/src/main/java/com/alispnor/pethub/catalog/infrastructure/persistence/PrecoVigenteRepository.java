package com.alispnor.pethub.catalog.infrastructure.persistence;

import com.alispnor.pethub.catalog.domain.entity.PrecoVigente;
import com.alispnor.pethub.catalog.domain.entity.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrecoVigenteRepository extends JpaRepository<PrecoVigente, Long> {

    @Query("SELECT p FROM PrecoVigente p WHERE p.produto = :produto AND p.dataFim IS NULL")
    Optional<PrecoVigente> findVigenteByProduto(@Param("produto") Produto produto);
}
