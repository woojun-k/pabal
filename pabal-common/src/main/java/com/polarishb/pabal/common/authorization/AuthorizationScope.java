package com.polarishb.pabal.common.authorization;

import java.util.Objects;
import java.util.UUID;

public record AuthorizationScope(
        String type,
        String id
) {

    public AuthorizationScope {
        type = requireText(type, "type");
        id = requireText(id, "id");
    }

    public static AuthorizationScope of(String type, UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return new AuthorizationScope(type, id.toString());
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return trimmed;
    }
}
