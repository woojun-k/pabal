package com.polarishb.pabal.security.config;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

final class JwtClaimValidators {

    private JwtClaimValidators() {
    }

    static OAuth2TokenValidator<Jwt> audience(String requiredAudience) {
        return jwt -> jwt.getAudience().contains(requiredAudience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "Missing required audience", null)
        );
    }

    static OAuth2TokenValidator<Jwt> requiredClaims(List<String> claims) {
        return jwt -> claims.stream().allMatch(claim -> hasRequiredClaim(jwt, claim))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "Missing required claims", null)
        );
    }

    private static boolean hasRequiredClaim(Jwt jwt, String claim) {
        return jwt.getClaims().containsKey(claim) && jwt.getClaims().get(claim) != null;
    }
}
