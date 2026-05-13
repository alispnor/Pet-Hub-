package com.alispnor.pethub.pricing.infrastructure.persistence;

import com.alispnor.pethub.pricing.domain.entity.CupomUso;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CupomUsoRepository extends JpaRepository<CupomUso, Long> {

    long countByCupomId(Long cupomId);

    long countByCupomIdAndUsuarioId(Long cupomId, Long usuarioId);
}
