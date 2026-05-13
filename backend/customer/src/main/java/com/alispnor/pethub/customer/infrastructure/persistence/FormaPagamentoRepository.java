package com.alispnor.pethub.customer.infrastructure.persistence;

import com.alispnor.pethub.customer.domain.entity.FormaPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FormaPagamentoRepository extends JpaRepository<FormaPagamento, Long> {

    List<FormaPagamento> findAllByPerfilClienteIdOrderByCriadoEmDesc(Long perfilClienteId);

    @Modifying
    @Query("UPDATE FormaPagamento f SET f.padrao = false " +
            "WHERE f.perfilCliente.id = :perfilId AND f.padrao = true AND f.id <> :keepId")
    int clearOtherDefault(@Param("perfilId") Long perfilId, @Param("keepId") Long keepId);

    long countByPerfilClienteId(Long perfilClienteId);
}
