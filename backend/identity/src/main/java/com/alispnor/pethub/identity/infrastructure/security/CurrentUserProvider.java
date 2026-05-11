package com.alispnor.pethub.identity.infrastructure.security;

import com.alispnor.pethub.common.exception.BusinessRuleException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Helper para obter o usuário autenticado a partir do SecurityContext.
 */
@Component
public class CurrentUserProvider {

    public Optional<AuthenticatedUser> current() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        var principal = auth.getPrincipal();
        if (principal instanceof AuthenticatedUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public AuthenticatedUser requireCurrent() {
        return current().orElseThrow(() -> new BusinessRuleException("Usuário não autenticado"));
    }
}
