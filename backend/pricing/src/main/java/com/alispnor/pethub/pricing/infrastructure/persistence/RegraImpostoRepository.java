package com.alispnor.pethub.pricing.infrastructure.persistence;

import com.alispnor.pethub.pricing.domain.entity.RegraImposto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface RegraImpostoRepository extends JpaRepository<RegraImposto, Long> {

    @Query("""
            SELECT r FROM RegraImposto r
            WHERE r.ncm = :ncm
              AND r.ufOrigem = :ufOrigem
              AND r.ufDestino = :ufDestino
              AND r.vigenciaInicio <= :data
              AND (r.vigenciaFim IS NULL OR r.vigenciaFim >= :data)
            ORDER BY r.vigenciaInicio DESC
            """)
    Optional<RegraImposto> findVigente(@Param("ncm") String ncm,
                                       @Param("ufOrigem") String ufOrigem,
                                       @Param("ufDestino") String ufDestino,
                                       @Param("data") LocalDate data);
}
