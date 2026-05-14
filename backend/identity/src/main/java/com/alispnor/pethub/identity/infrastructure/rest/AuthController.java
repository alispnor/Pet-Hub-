package com.alispnor.pethub.identity.infrastructure.rest;

import com.alispnor.pethub.common.exception.BusinessRuleException;
import com.alispnor.pethub.identity.application.dto.AuthResponse;
import com.alispnor.pethub.identity.application.dto.LoginRequest;
import com.alispnor.pethub.identity.application.dto.RegisterAdminRequest;
import com.alispnor.pethub.identity.application.dto.RegisterClienteRequest;
import com.alispnor.pethub.identity.application.dto.TokenRefreshRequest;
import com.alispnor.pethub.identity.application.dto.UsuarioResponse;
import com.alispnor.pethub.identity.application.usecase.AuthService;
import com.alispnor.pethub.identity.infrastructure.security.CurrentUserProvider;
import com.alispnor.pethub.identity.infrastructure.security.JwtService;
import com.alispnor.pethub.identity.infrastructure.security.RefreshCookieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Autenticação e gestão de tokens (refresh em cookie HttpOnly)")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserProvider currentUserProvider;
    private final RefreshCookieService refreshCookieService;
    private final JwtService jwtService;

    @PostMapping("/register/cliente")
    @Operation(summary = "Registra um cliente final")
    public ResponseEntity<UsuarioResponse> registerCliente(@Valid @RequestBody RegisterClienteRequest request) {
        var response = authService.registerCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/register/admin")
    @Operation(summary = "Registra um admin (apenas ADMIN_LOJA pode chamar)")
    public ResponseEntity<UsuarioResponse> registerAdmin(@Valid @RequestBody RegisterAdminRequest request) {
        var response = authService.registerAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Login com email + senha; access token no body, refresh no cookie HttpOnly")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        var emitted = authService.login(request);
        refreshCookieService.setCookie(response, emitted.refreshToken(), jwtService.refreshTokenTtl());
        return stripRefresh(emitted);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Troca o refresh token por um novo par. Cookie tem prioridade sobre body.")
    public AuthResponse refresh(@RequestBody(required = false) TokenRefreshRequest request,
                                HttpServletRequest httpRequest,
                                HttpServletResponse response) {
        var refreshToken = resolveRefreshToken(request, httpRequest);
        var emitted = authService.refresh(new TokenRefreshRequest(refreshToken));
        refreshCookieService.setCookie(response, emitted.refreshToken(), jwtService.refreshTokenTtl());
        return stripRefresh(emitted);
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoga o refresh token (cookie ou body) e zera o cookie HttpOnly")
    public ResponseEntity<Void> logout(@RequestBody(required = false) TokenRefreshRequest request,
                                       HttpServletRequest httpRequest,
                                       HttpServletResponse response) {
        // Logout é idempotente: se não houver token, só zera o cookie e devolve 204.
        var token = resolveRefreshTokenOptional(request, httpRequest);
        if (token != null) {
            authService.logout(new TokenRefreshRequest(token));
        }
        refreshCookieService.clearCookie(response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Retorna o usuário autenticado")
    public UsuarioResponse me() {
        var current = currentUserProvider.requireCurrent();
        return authService.me(current.id());
    }

    // ---------------------------------------------------------- helpers

    private String resolveRefreshToken(TokenRefreshRequest body, HttpServletRequest httpRequest) {
        var token = resolveRefreshTokenOptional(body, httpRequest);
        if (token == null) {
            throw new BusinessRuleException(
                    "Refresh token ausente: envie via cookie HttpOnly '" + refreshCookieService.cookieName()
                    + "' ou no body como {\"refreshToken\":\"...\"}");
        }
        return token;
    }

    private String resolveRefreshTokenOptional(TokenRefreshRequest body, HttpServletRequest httpRequest) {
        var fromCookie = refreshCookieService.extract(httpRequest);
        if (fromCookie.isPresent()) {
            return fromCookie.get();
        }
        if (body != null && body.refreshToken() != null && !body.refreshToken().isBlank()) {
            return body.refreshToken();
        }
        return null;
    }

    /**
     * Remove o refresh token do body antes de devolver para o cliente. Em
     * produção apenas o cookie HttpOnly carrega o valor; expor no JSON anularia
     * a proteção contra XSS exigida pelo AppSec Guidelines.
     */
    private AuthResponse stripRefresh(AuthResponse emitted) {
        return new AuthResponse(emitted.accessToken(), null, emitted.expiresIn(), emitted.usuario());
    }
}
