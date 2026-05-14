package com.alispnor.pethub.order.infrastructure.persistence;

import com.alispnor.pethub.order.domain.entity.Pedido;
import com.alispnor.pethub.order.domain.entity.StatusPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findByNumeroPedido(String numeroPedido);

    Page<Pedido> findByClienteId(Long clienteId, Pageable pageable);

    @Query("""
        select p from Pedido p
        where p.cliente.id = :clienteId
          and (:status is null or p.status = :status)
          and (:dataInicio is null or p.criadoEm >= :dataInicio)
          and (:dataFim is null or p.criadoEm <= :dataFim)
        """)
    Page<Pedido> searchByCliente(
            @Param("clienteId") Long clienteId,
            @Param("status") StatusPedido status,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            Pageable pageable
    );

    @Query("""
        select p from Pedido p
        where (:status is null or p.status = :status)
          and (cast(:q as string) is null
               or lower(p.numeroPedido) like lower(concat('%', cast(:q as string), '%')))
        """)
    Page<Pedido> searchAdmin(
            @Param("status") StatusPedido status,
            @Param("q") String q,
            Pageable pageable
    );

    @Query("select p.status, count(p) from Pedido p group by p.status")
    List<Object[]> countByStatus();
}
