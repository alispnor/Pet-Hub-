package com.alispnor.pethub.inventory.infrastructure.persistence;

import com.alispnor.pethub.inventory.domain.entity.ReservaEstoque;
import com.alispnor.pethub.inventory.domain.entity.StatusReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservaEstoqueRepository extends JpaRepository<ReservaEstoque, Long> {

    List<ReservaEstoque> findByReferenciaPedidoAndStatus(String referenciaPedido, StatusReserva status);

    @Query("select r from ReservaEstoque r where r.status = :status and r.expiraEm < :ate")
    List<ReservaEstoque> findExpiradas(@Param("status") StatusReserva status, @Param("ate") LocalDateTime ate);
}
