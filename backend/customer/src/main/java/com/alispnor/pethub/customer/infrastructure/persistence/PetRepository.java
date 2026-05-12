package com.alispnor.pethub.customer.infrastructure.persistence;

import com.alispnor.pethub.customer.domain.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PetRepository extends JpaRepository<Pet, Long> {

    List<Pet> findAllByPerfilClienteIdOrderByNomeAsc(Long perfilClienteId);
}
