package com.polarishb.pabal.messenger.infrastructure.realtime.ws.security;

import com.polarishb.pabal.security.authentication.PabalPrincipal;
import org.springframework.messaging.simp.user.DestinationUserNameProvider;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Objects;

public final class StompAuthenticationToken extends AbstractAuthenticationToken implements DestinationUserNameProvider {

    private final Authentication delegate;
    private final PabalPrincipal principal;

    public StompAuthenticationToken(Authentication delegate) {
        super(Objects.requireNonNull(delegate, "delegate must not be null").getAuthorities());
        if (!(delegate.getPrincipal() instanceof PabalPrincipal pabalPrincipal)) {
            throw new IllegalArgumentException("delegate principal must be PabalPrincipal");
        }
        this.delegate = delegate;
        this.principal = pabalPrincipal;
        setDetails(delegate.getDetails());
        setAuthenticated(delegate.isAuthenticated());
    }

    @Override
    public Object getCredentials() {
        return delegate.getCredentials();
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public String getName() {
        return principal.getName();
    }

    @Override
    public String getDestinationUserName() {
        return RealtimePrincipal.destinationUserName(principal.tenantId(), principal.userId());
    }
}
