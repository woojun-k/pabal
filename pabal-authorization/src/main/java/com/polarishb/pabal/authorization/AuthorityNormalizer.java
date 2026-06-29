package com.polarishb.pabal.authorization;

import com.polarishb.pabal.common.authorization.FineGrainedPermission;

import java.util.Locale;
import java.util.Objects;

public final class AuthorityNormalizer {

    public static final String ROLE_PREFIX = "ROLE_";
    public static final String SCOPE_PREFIX = "SCOPE_";
    public static final String PERMISSION_PREFIX = "PERMISSION_";

    private AuthorityNormalizer() {
    }

    public static String role(String role) {
        String normalized = token(role);
        return normalized.startsWith(ROLE_PREFIX) ? normalized : ROLE_PREFIX + normalized;
    }

    /**
     * Normalized permission alias for coarse external systems that cannot carry colon-separated authorities.
     * Example: {@code messenger:channel:create} becomes {@code PERMISSION_MESSENGER_CHANNEL_CREATE}.
     */
    public static String permissionAlias(FineGrainedPermission permission) {
        Objects.requireNonNull(permission, "permission must not be null");
        return PERMISSION_PREFIX + token(permission.value());
    }

    /**
     * Spring OAuth2 resource server keeps JWT scope values as-is and prefixes them with {@code SCOPE_}.
     */
    public static String scopeAuthority(String authority) {
        String value = requireText(authority, "authority");
        return SCOPE_PREFIX + value;
    }

    /**
     * Converts role-like tokens to uppercase underscore form so {@code tenant:{uuid}:admin},
     * {@code tenant-{uuid}-admin}, and {@code TENANT_{uuid}_ADMIN} compare consistently.
     */
    public static String token(String value) {
        return requireText(value, "value")
                .toUpperCase(Locale.ROOT)
                .replace(':', '_')
                .replace('-', '_');
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
