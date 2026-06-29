package com.polarishb.pabal.security.authentication;

import com.polarishb.pabal.authorization.AuthorityNormalizer;
import com.polarishb.pabal.security.config.JwtSecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
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
        String userIdRaw = requiredClaim(jwt, properties.userIdClaim());
        String tenantIdRaw = requiredClaim(jwt, properties.tenantIdClaim());
        String subject = requiredClaim(jwt, properties.principalClaim());

        PabalPrincipal principal = new PabalPrincipal(
                parseUuid(userIdRaw, properties.userIdClaim()),
                parseUuid(tenantIdRaw, properties.tenantIdClaim()),
                subject
        );

        Collection<GrantedAuthority> authorities = authorities(jwt);

        return new PabalJwtAuthenticationToken(principal, jwt, authorities);
    }

    private String requiredClaim(Jwt jwt, String claimName) {
        String value = jwt.getClaimAsString(claimName);
        if (value == null) {
            throw invalidToken("Required JWT claim '%s' is missing".formatted(claimName));
        }
        return value;
    }

    private UUID parseUuid(String value, String claimName) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw invalidToken("JWT claim '%s' is not a valid UUID: %s".formatted(claimName, value));
        }
    }

    private OAuth2AuthenticationException invalidToken(String message) {
        return new OAuth2AuthenticationException(new OAuth2Error(
                OAuth2ErrorCodes.INVALID_TOKEN,
                message,
                null
        ));
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
            authorities.add(AuthorityNormalizer.role(role));
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
}
