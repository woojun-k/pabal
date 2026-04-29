package com.polarishb.pabal.security.dev;

import com.polarishb.pabal.security.config.JwtSecurityProperties;
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
    private final JwtEncoder jwtEncoder;
    private final JwtSecurityProperties jwtProperties;

    @GetMapping("/dev/token")
    public Map<String, String> token(
            @RequestParam UUID userId,
            @RequestParam UUID tenantId
    ) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("local-dev")
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60 * 60))
                .audience(List.of(jwtProperties.audience()))
                .claim(jwtProperties.userIdClaim(), userId.toString())
                .claim(jwtProperties.tenantIdClaim(), tenantId.toString())
                .claim(jwtProperties.principalClaim(), userId.toString())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        String accessToken = jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();

        return Map.of("accessToken", accessToken);
    }
}
