package com.alispnor.pethub.identity.infrastructure.persistence;

import com.alispnor.pethub.identity.domain.entity.TipoUsuario;
import com.alispnor.pethub.identity.domain.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByCpf(String cpf);

    Page<Usuario> findByTipo(TipoUsuario tipo, Pageable pageable);

    @Query("""
            SELECT u FROM Usuario u
            WHERE u.tipo = :tipo
              AND ( LOWER(u.nome) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')) )
            """)
    Page<Usuario> searchByTipo(@Param("tipo") TipoUsuario tipo,
                               @Param("q") String q,
                               Pageable pageable);
}
