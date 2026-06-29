package com.polarishb.pabal.messenger.infrastructure.realtime.ws.security;

import org.springframework.messaging.simp.user.DestinationUserNameProvider;

import java.io.Serial;
import java.io.Serializable;
import java.security.Principal;
import java.util.UUID;

public record RealtimePrincipal(
    UUID userId,
    UUID tenantId
) implements Principal, Serializable, DestinationUserNameProvider {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public String getName() {
        return userId.toString();
    }

    @Override
    public String getDestinationUserName() {
        return destinationUserName(tenantId, userId);
    }

    public static String destinationUserName(UUID tenantId, UUID userId) {
        return tenantId + ":" + userId;
    }
}
