package com.polarishb.pabal.security.dev;

import com.polarishb.pabal.security.config.JwtSecurityProperties;
import com.polarishb.pabal.security.token.JwtAuthorityClaims;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Profile({"local", "test"})
public class LocalDevTokenController {
    private final JwtSecurityProperties jwtProperties;
    private final JwtEncoder jwtEncoder;

    @GetMapping("/dev/token")
    public Map<String, String> token(
            @RequestParam UUID userId,
            @RequestParam UUID tenantId,
            @RequestParam(required = false, name = "scope") List<String> scopes,
            @RequestParam(required = false, name = "role") List<String> roles,
            @RequestParam(required = false, name = "permission") List<String> permissions
    ) {
        JwtAuthorityClaims authorityClaims = JwtAuthorityClaims.of(scopes, roles, permissions);
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProperties.accessTokenMaxTtl());
        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuerUri())
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .audience(List.of(jwtProperties.audience()))
                .claim(jwtProperties.userIdClaim(), userId.toString())
                .claim(jwtProperties.tenantIdClaim(), tenantId.toString())
                .claim(jwtProperties.principalClaim(), userId.toString());

        if (!authorityClaims.scopes().isEmpty()) {
            claimsBuilder.claim("scope", String.join(" ", authorityClaims.scopes()));
        }
        if (!authorityClaims.roles().isEmpty()) {
            claimsBuilder.claim("roles", authorityClaims.roles());
        }
        if (!authorityClaims.permissions().isEmpty()) {
            claimsBuilder.claim("permissions", authorityClaims.permissions());
        }

        String accessToken = jwtEncoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claimsBuilder.build())
        ).getTokenValue();

        return Map.of(
                "tokenType", "Bearer",
                "accessToken", accessToken,
                "accessTokenExpiresAt", expiresAt.toString(),
                "audience", jwtProperties.audience()
        );
    }
}
