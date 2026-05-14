package com.alispnor.pethub.checkout.infrastructure.persistence;

import com.alispnor.pethub.checkout.domain.entity.TentativaPagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TentativaPagamentoRepository extends JpaRepository<TentativaPagamento, Long> {

    Optional<TentativaPagamento> findByIdempotencyKey(String idempotencyKey);

    Optional<TentativaPagamento> findByReferenciaPedido(String referenciaPedido);
}
