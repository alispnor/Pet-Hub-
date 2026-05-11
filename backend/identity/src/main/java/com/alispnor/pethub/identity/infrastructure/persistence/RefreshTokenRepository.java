package com.alispnor.pethub.identity.infrastructure.persistence;

import com.alispnor.pethub.identity.domain.entity.RefreshToken;
import com.alispnor.pethub.identity.domain.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revogado = true WHERE r.usuario = :usuario AND r.revogado = false")
    int revogarTodosDoUsuario(@Param("usuario") Usuario usuario);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiraEm < :limite")
    int deletarExpirados(@Param("limite") LocalDateTime limite);
}
