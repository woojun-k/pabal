package com.polarishb.pabal.security.authentication;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.messaging.simp.user.DestinationUserNameProvider;

import java.util.Collection;

public class PabalJwtAuthenticationToken extends AbstractAuthenticationToken implements DestinationUserNameProvider {

    private final PabalPrincipal principal;
    private final Jwt jwt;

    public PabalJwtAuthenticationToken(
            PabalPrincipal principal,
            Jwt jwt,
            Collection<? extends GrantedAuthority> authorities
    ) {
        super(authorities);
        this.principal = principal;
        this.jwt = jwt;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    public Jwt getJwt() {
        return jwt;
    }

    @Override
    public String getName() {
        return principal.getName();
    }

    @Override
    public String getDestinationUserName() {
        return principal.getDestinationUserName();
    }
}
