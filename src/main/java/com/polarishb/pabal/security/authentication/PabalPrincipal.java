package com.polarishb.pabal.security.authentication;

import org.springframework.messaging.simp.user.DestinationUserNameProvider;

import java.io.Serial;
import java.io.Serializable;
import java.security.Principal;
import java.util.UUID;

public record PabalPrincipal(
    UUID userId,
    UUID tenantId,
    String subject
) implements Principal, Serializable, DestinationUserNameProvider {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public String getName() {
        return subject;
    }

    @Override
    public String getDestinationUserName() {
        return destinationUserName(tenantId, userId);
    }

    public static String destinationUserName(UUID tenantId, UUID userId) {
        return tenantId + ":" + userId;
    }
}
