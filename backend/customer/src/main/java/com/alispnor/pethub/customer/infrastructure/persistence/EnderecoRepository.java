package com.alispnor.pethub.customer.infrastructure.persistence;

import com.alispnor.pethub.customer.domain.entity.Endereco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EnderecoRepository extends JpaRepository<Endereco, Long> {

    List<Endereco> findAllByPerfilClienteIdOrderByApelidoAsc(Long perfilClienteId);

    @Modifying
    @Query("UPDATE Endereco e SET e.padraoEntrega = false " +
            "WHERE e.perfilCliente.id = :perfilId AND e.padraoEntrega = true AND e.id <> :keepId")
    int clearOtherDefaultEntrega(@Param("perfilId") Long perfilId, @Param("keepId") Long keepId);

    @Modifying
    @Query("UPDATE Endereco e SET e.padraoCobranca = false " +
            "WHERE e.perfilCliente.id = :perfilId AND e.padraoCobranca = true AND e.id <> :keepId")
    int clearOtherDefaultCobranca(@Param("perfilId") Long perfilId, @Param("keepId") Long keepId);

    long countByPerfilClienteId(Long perfilClienteId);
}
