package com.alispnor.pethub.customer.infrastructure.persistence;

import com.alispnor.pethub.customer.domain.entity.PerfilCliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PerfilClienteRepository extends JpaRepository<PerfilCliente, Long> {

    Optional<PerfilCliente> findByUsuarioId(Long usuarioId);

    Optional<PerfilCliente> findByCpfHash(String cpfHash);

    boolean existsByCpfHash(String cpfHash);
}
