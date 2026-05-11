package com.alispnor.pethub.identity.infrastructure.rest;

import com.alispnor.pethub.identity.application.dto.AuthResponse;
import com.alispnor.pethub.identity.application.dto.LoginRequest;
import com.alispnor.pethub.identity.application.dto.RegisterAdminRequest;
import com.alispnor.pethub.identity.application.dto.RegisterClienteRequest;
import com.alispnor.pethub.identity.application.dto.TokenRefreshRequest;
import com.alispnor.pethub.identity.application.dto.UsuarioResponse;
import com.alispnor.pethub.identity.application.usecase.AuthService;
import com.alispnor.pethub.identity.infrastructure.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Auth", description = "Autenticação e gestão de tokens")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserProvider currentUserProvider;

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
    @Operation(summary = "Login com email + senha; retorna access + refresh token")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Troca o refresh token por um novo par (rotação)")
    public AuthResponse refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoga o refresh token informado")
    public ResponseEntity<Void> logout(@Valid @RequestBody TokenRefreshRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Retorna o usuário autenticado")
    public UsuarioResponse me() {
        var current = currentUserProvider.requireCurrent();
        return authService.me(current.id());
    }
}
