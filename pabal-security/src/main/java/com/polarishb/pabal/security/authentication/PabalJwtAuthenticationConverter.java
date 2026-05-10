package com.polarishb.pabal.security.authentication;

import com.polarishb.pabal.security.config.JwtSecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PabalJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtSecurityProperties properties;

    private final JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String userIdRaw = jwt.getClaimAsString(properties.userIdClaim());
        String tenantIdRaw = jwt.getClaimAsString(properties.tenantIdClaim());
        String subject = jwt.getClaimAsString(properties.principalClaim());

        if (userIdRaw == null || tenantIdRaw == null || subject == null) {
            throw new IllegalArgumentException("Required JWT claims are missing");
        }

        PabalPrincipal principal = new PabalPrincipal(
                UUID.fromString(userIdRaw),
                UUID.fromString(tenantIdRaw),
                subject
        );

        Collection<GrantedAuthority> authorities = authorities(jwt);

        return new PabalJwtAuthenticationToken(principal, jwt, authorities);
    }

    private Collection<GrantedAuthority> authorities(Jwt jwt) {
        Map<String, GrantedAuthority> merged = new LinkedHashMap<>();
        Collection<GrantedAuthority> scopeAuthorities = authoritiesConverter.convert(jwt);

        if (scopeAuthorities != null) {
            for (GrantedAuthority authority : scopeAuthorities) {
                merged.put(authority.getAuthority(), authority);
            }
        }

        for (String authority : additionalAuthorityValues(jwt)) {
            merged.putIfAbsent(authority, new SimpleGrantedAuthority(authority));
        }

        return merged.values();
    }

    private List<String> additionalAuthorityValues(Jwt jwt) {
        List<String> authorities = new ArrayList<>();
        addPermissions(authorities, jwt.getClaim("permissions"));
        addRoles(authorities, jwt.getClaim("roles"));
        addKeycloakRealmRoles(authorities, jwt.getClaim("realm_access"));
        addKeycloakResourceRoles(authorities, jwt.getClaim("resource_access"));
        return authorities;
    }

    private void addPermissions(List<String> authorities, Object claim) {
        authorities.addAll(claimValues(claim));
    }

    private void addRoles(List<String> authorities, Object claim) {
        for (String role : claimValues(claim)) {
            authorities.add(normalizeRole(role));
        }
    }

    private void addKeycloakRealmRoles(List<String> authorities, Object claim) {
        if (claim instanceof Map<?, ?> realmAccess) {
            addRoles(authorities, realmAccess.get("roles"));
        }
    }

    private void addKeycloakResourceRoles(List<String> authorities, Object claim) {
        if (!(claim instanceof Map<?, ?> resourceAccess)) {
            return;
        }

        for (Object resource : resourceAccess.values()) {
            if (resource instanceof Map<?, ?> resourceClaims) {
                addRoles(authorities, resourceClaims.get("roles"));
            }
        }
    }

    private List<String> claimValues(Object claim) {
        if (claim instanceof String value) {
            return Arrays.stream(value.split("[\\s,]+"))
                    .filter(s -> !s.isBlank())
                    .toList();
        }

        if (claim instanceof Collection<?> values) {
            return values.stream()
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();
        }

        return List.of();
    }

    private String normalizeRole(String role) {
        String value = role.trim().toUpperCase().replace('-', '_');
        return value.startsWith("ROLE_") ? value : "ROLE_" + value;
    }
}
