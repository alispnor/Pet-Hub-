package com.alispnor.pethub.pricing.infrastructure.persistence;

import com.alispnor.pethub.pricing.domain.entity.Promocao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface PromocaoRepository extends JpaRepository<Promocao, Long> {

    @Query("""
            SELECT p FROM Promocao p
            WHERE p.ativo = true
              AND p.dataInicio <= :now
              AND (p.dataFim IS NULL OR p.dataFim > :now)
            """)
    List<Promocao> findVigentes(LocalDateTime now);
}
