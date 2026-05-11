package com.alispnor.pethub.identity.infrastructure.security;

import com.alispnor.pethub.common.exception.BusinessRuleException;
import com.alispnor.pethub.identity.domain.entity.Role;
import com.alispnor.pethub.identity.domain.entity.TipoUsuario;
import com.alispnor.pethub.identity.domain.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Set;

/**
 * Geração e validação de tokens. O access token é JWT com claims do usuário.
 * O refresh token é uma string opaca aleatória (não JWT); o hash SHA-256 é
 * persistido em refresh_tokens, e o token cru é entregue ao cliente.
 */
@Slf4j
@Service
public class JwtService {

    private static final String CLAIM_TIPO = "tipo";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_NOME = "nome";

    private final SecretKey signingKey;
    private final Duration accessTtl;
    private final Duration refreshTtl;
    private final SecureRandom random = new SecureRandom();

    public JwtService(JwtProperties props) {
        var keyBytes = props.secret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret deve ter no mínimo 32 bytes (256 bits) para HS256");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTtl = Duration.ofMinutes(props.accessTokenTtlMinutes());
        this.refreshTtl = Duration.ofDays(props.refreshTokenTtlDays());
    }

    public String generateAccessToken(Usuario usuario) {
        log.debug("Gerando access token para usuario id={}", usuario.getId());
        var now = Instant.now();
        var token = Jwts.builder()
                .subject(String.valueOf(usuario.getId()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .claim(CLAIM_NOME, usuario.getNome())
                .claim(CLAIM_TIPO, usuario.getTipo().name())
                .claim(CLAIM_ROLES, usuario.getRoles().stream().map(Enum::name).toList())
                .claim("email", usuario.getEmail())
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
        log.debug("Access token gerado, expira em {}s", accessTtl.toSeconds());
        return token;
    }

    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new BusinessRuleException("Token expirado");
        } catch (Exception e) {
            log.debug("Falha ao validar token: {}", e.getMessage());
            throw new BusinessRuleException("Token inválido");
        }
    }

    public long extractUserId(Claims claims) {
        return Long.parseLong(claims.getSubject());
    }

    public TipoUsuario extractTipo(Claims claims) {
        return TipoUsuario.valueOf(claims.get(CLAIM_TIPO, String.class));
    }

    @SuppressWarnings("unchecked")
    public Set<Role> extractRoles(Claims claims) {
        var list = claims.get(CLAIM_ROLES, java.util.List.class);
        return ((java.util.List<String>) list).stream()
                .map(Role::valueOf)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public int accessTokenTtlSeconds() {
        return (int) accessTtl.toSeconds();
    }

    public Duration refreshTokenTtl() {
        return refreshTtl;
    }

    /**
     * Gera um refresh token opaco: 32 bytes aleatórios em base64 url-safe.
     */
    public String generateRefreshTokenRaw() {
        var bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256 do refresh token cru, para persistência segura.
     */
    public String hashRefreshToken(String raw) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            var digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível na JVM", e);
        }
    }
}
