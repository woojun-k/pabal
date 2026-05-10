package com.polarishb.pabal.security.context;

import com.polarishb.pabal.security.authentication.PabalPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SecurityContextCurrentAuthenticationProvider implements CurrentAuthenticationProvider {

    @Override
    public Optional<CurrentAuthentication> currentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        if (!(authentication.getPrincipal() instanceof PabalPrincipal principal)) {
            return Optional.empty();
        }

        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toUnmodifiableSet());

        return Optional.of(new CurrentAuthentication(principal, authorities));
    }
}
