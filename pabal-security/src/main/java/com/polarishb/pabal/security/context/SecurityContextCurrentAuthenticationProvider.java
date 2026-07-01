package com.polarishb.pabal.security.context;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SecurityContextCurrentAuthenticationProvider implements CurrentAuthenticationProvider {

    private final CurrentAuthenticationScope currentAuthenticationScope;
    private final CurrentAuthenticationResolver currentAuthenticationResolver;

    @Override
    public Optional<CurrentAuthentication> currentAuthentication() {
        return currentAuthenticationScope.currentAuthentication()
                .or(this::securityContextAuthentication);
    }

    private Optional<CurrentAuthentication> securityContextAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return currentAuthenticationResolver.resolve(authentication);
    }
}
