package com.alispnor.pethub.catalog.infrastructure.persistence;

import com.alispnor.pethub.catalog.domain.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    Optional<Categoria> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Categoria> findAllByOrderByOrdemAscNomeAsc();

    List<Categoria> findByAtivoTrueOrderByOrdemAscNomeAsc();
}
