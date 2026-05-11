package com.alispnor.pethub.identity.application.usecase;

import com.alispnor.pethub.common.exception.BusinessRuleException;
import com.alispnor.pethub.common.exception.ConflictException;
import com.alispnor.pethub.common.exception.ResourceNotFoundException;
import com.alispnor.pethub.identity.application.dto.AuthResponse;
import com.alispnor.pethub.identity.application.dto.LoginRequest;
import com.alispnor.pethub.identity.application.dto.RegisterAdminRequest;
import com.alispnor.pethub.identity.application.dto.RegisterClienteRequest;
import com.alispnor.pethub.identity.application.dto.TokenRefreshRequest;
import com.alispnor.pethub.identity.application.dto.UsuarioResponse;
import com.alispnor.pethub.identity.application.mapper.UsuarioMapper;
import com.alispnor.pethub.identity.domain.entity.RefreshToken;
import com.alispnor.pethub.identity.domain.entity.Role;
import com.alispnor.pethub.identity.domain.entity.TipoUsuario;
import com.alispnor.pethub.identity.domain.entity.Usuario;
import com.alispnor.pethub.identity.infrastructure.persistence.RefreshTokenRepository;
import com.alispnor.pethub.identity.infrastructure.persistence.UsuarioRepository;
import com.alispnor.pethub.identity.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UsuarioMapper usuarioMapper;

    @Transactional
    public UsuarioResponse registerCliente(RegisterClienteRequest request) {
        log.debug("Iniciando registerCliente para email={}", maskEmail(request.email()));
        validateEmailDisponivel(request.email());
        var usuario = Usuario.builder()
                .nome(request.nome())
                .email(request.email().toLowerCase())
                .senhaHash(passwordEncoder.encode(request.senha()))
                .telefone(request.telefone())
                .tipo(TipoUsuario.CLIENTE)
                .roles(Set.of(Role.ROLE_CLIENTE))
                .ativo(true)
                .build();
        var salvo = usuarioRepository.save(usuario);
        var response = usuarioMapper.toResponse(salvo);
        log.debug("Cliente registrado id={}", salvo.getId());
        return response;
    }

    @Transactional
    public UsuarioResponse registerAdmin(RegisterAdminRequest request) {
        log.debug("Iniciando registerAdmin para email={}", maskEmail(request.email()));
        validateEmailDisponivel(request.email());
        if (request.roles().contains(Role.ROLE_CLIENTE)) {
            throw new BusinessRuleException("Admin não pode ter ROLE_CLIENTE");
        }
        var usuario = Usuario.builder()
                .nome(request.nome())
                .email(request.email().toLowerCase())
                .senhaHash(passwordEncoder.encode(request.senha()))
                .tipo(TipoUsuario.ADMIN)
                .roles(request.roles())
                .ativo(true)
                .build();
        var salvo = usuarioRepository.save(usuario);
        var response = usuarioMapper.toResponse(salvo);
        log.debug("Admin registrado id={} roles={}", salvo.getId(), salvo.getRoles());
        return response;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.debug("Iniciando login para email={}", maskEmail(request.email()));
        var usuario = usuarioRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));
        if (!usuario.isAtivo()) {
            throw new BadCredentialsException("Credenciais inválidas");
        }
        if (usuario.estaBloqueado()) {
            throw new BusinessRuleException("Conta temporariamente bloqueada");
        }
        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            usuario.registrarLoginFalha(5, 15);
            usuarioRepository.save(usuario);
            throw new BadCredentialsException("Credenciais inválidas");
        }
        usuario.registrarLoginSucesso();
        usuarioRepository.save(usuario);
        var response = emitTokens(usuario);
        log.debug("Login OK usuario id={}", usuario.getId());
        return response;
    }

    @Transactional
    public AuthResponse refresh(TokenRefreshRequest request) {
        log.debug("Iniciando refresh");
        var hash = jwtService.hashRefreshToken(request.refreshToken());
        var token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessRuleException("Refresh token inválido"));
        if (!token.estaValido()) {
            throw new BusinessRuleException("Refresh token expirado ou revogado");
        }
        // Rotação: revoga o atual e emite um novo par
        token.setRevogado(true);
        refreshTokenRepository.save(token);
        var response = emitTokens(token.getUsuario());
        log.debug("Refresh OK usuario id={}", token.getUsuario().getId());
        return response;
    }

    @Transactional
    public void logout(TokenRefreshRequest request) {
        log.debug("Iniciando logout");
        var hash = jwtService.hashRefreshToken(request.refreshToken());
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setRevogado(true);
            refreshTokenRepository.save(token);
            log.debug("Logout OK usuario id={}", token.getUsuario().getId());
        });
    }

    @Transactional(readOnly = true)
    public UsuarioResponse me(long userId) {
        log.debug("Iniciando me userId={}", userId);
        var usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + userId));
        var response = usuarioMapper.toResponse(usuario);
        log.debug("me OK userId={}", userId);
        return response;
    }

    private AuthResponse emitTokens(Usuario usuario) {
        var accessToken = jwtService.generateAccessToken(usuario);
        var refreshRaw = jwtService.generateRefreshTokenRaw();
        var refreshHash = jwtService.hashRefreshToken(refreshRaw);
        var refreshEntity = RefreshToken.builder()
                .usuario(usuario)
                .tokenHash(refreshHash)
                .expiraEm(LocalDateTime.now().plus(jwtService.refreshTokenTtl()))
                .revogado(false)
                .build();
        refreshTokenRepository.save(refreshEntity);
        return new AuthResponse(
                accessToken,
                refreshRaw,
                jwtService.accessTokenTtlSeconds(),
                usuarioMapper.toResponse(usuario)
        );
    }

    private void validateEmailDisponivel(String email) {
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email já cadastrado: " + maskEmail(email));
        }
    }

    private String maskEmail(String email) {
        if (email == null) {
            return null;
        }
        var at = email.indexOf('@');
        if (at <= 1) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
