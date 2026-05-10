package com.polarishb.pabal.security.context;

import com.polarishb.pabal.security.authentication.PabalPrincipal;

import java.util.Set;

public record CurrentAuthentication(
        PabalPrincipal principal,
        Set<String> authorities
) {

    public CurrentAuthentication {
        authorities = Set.copyOf(authorities);
    }
}
