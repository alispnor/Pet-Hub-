package com.alispnor.pethub.order.infrastructure.persistence;

import com.alispnor.pethub.order.domain.entity.PedidoSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PedidoSequenceRepository extends JpaRepository<PedidoSequence, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from PedidoSequence s where s.ano = :ano")
    Optional<PedidoSequence> findByAnoForUpdate(@Param("ano") Integer ano);
}
