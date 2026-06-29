package com.polarishb.pabal.security.token;

import java.util.List;

public record JwtAuthorityClaims(
        List<String> scopes,
        List<String> roles,
        List<String> permissions
) {

    public JwtAuthorityClaims {
        scopes = copy(scopes);
        roles = copy(roles);
        permissions = copy(permissions);
    }

    public static JwtAuthorityClaims of(
            List<String> scopes,
            List<String> roles,
            List<String> permissions
    ) {
        return new JwtAuthorityClaims(scopes, roles, permissions);
    }

    public static JwtAuthorityClaims empty() {
        return new JwtAuthorityClaims(List.of(), List.of(), List.of());
    }

    private static List<String> copy(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }
}
