package com.alispnor.pethub.shipping.infrastructure.persistence;

import com.alispnor.pethub.shipping.domain.entity.FaixaCepRegiao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FaixaCepRegiaoRepository extends JpaRepository<FaixaCepRegiao, Long> {

    @Query("""
            SELECT f FROM FaixaCepRegiao f
            WHERE :cep BETWEEN f.cepInicio AND f.cepFim
            """)
    Optional<FaixaCepRegiao> findByCep(@Param("cep") String cep);
}
