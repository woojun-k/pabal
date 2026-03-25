package com.polarishb.pabal.security.authentication;

import java.io.Serial;
import java.io.Serializable;
import java.security.Principal;
import java.util.UUID;

public record PabalPrincipal(
    UUID userId,
    UUID tenantId,
    String subject
) implements Principal, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public String getName() {
        return subject;
    }
}