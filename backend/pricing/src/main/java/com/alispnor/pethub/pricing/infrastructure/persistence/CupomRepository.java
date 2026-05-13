package com.alispnor.pethub.pricing.infrastructure.persistence;

import com.alispnor.pethub.pricing.domain.entity.Cupom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CupomRepository extends JpaRepository<Cupom, Long> {

    Optional<Cupom> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);

    Page<Cupom> findByAtivo(boolean ativo, Pageable pageable);
}
