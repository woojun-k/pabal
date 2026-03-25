package com.polarishb.pabal.security.authentication;

import com.polarishb.pabal.security.config.JwtSecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;
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

        Collection<GrantedAuthority> authorities = authoritiesConverter.convert(jwt);

        return new PabalJwtAuthenticationToken(principal, jwt, authorities);
    }
}
