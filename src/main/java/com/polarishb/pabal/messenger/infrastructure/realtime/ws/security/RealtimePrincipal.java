package com.polarishb.pabal.messenger.infrastructure.realtime.ws.security;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

public record RealtimePrincipal(
    UUID userId,
    UUID tenantId
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public String getName() {
        return userId.toString();
    }
}
