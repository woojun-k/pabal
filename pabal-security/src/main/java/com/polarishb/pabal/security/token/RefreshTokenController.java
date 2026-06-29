package com.polarishb.pabal.security.token;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/tokens")
public class RefreshTokenController {

    private final RefreshTokenService refreshTokenService;

    @PostMapping("/refresh")
    public TokenResponse refresh(
            @RequestHeader(name = "X-Request-ID", required = false) String requestId,
            @RequestBody RefreshTokenRequest request
    ) {
        try {
            return TokenResponse.from(refreshTokenService.refresh(request.refreshToken(), requestId));
        } catch (InvalidRefreshTokenException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ex.getMessage(), ex);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), ex);
        }
    }

    @PostMapping("/revoke")
    public ResponseEntity<Void> revoke(@RequestBody RefreshTokenRequest request) {
        try {
            refreshTokenService.revoke(request.refreshToken());
        } catch (InvalidRefreshTokenException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ex.getMessage(), ex);
        }
        return ResponseEntity.noContent().build();
    }

    public record RefreshTokenRequest(
            String refreshToken
    ) {
    }

    public record TokenResponse(
            String tokenType,
            String accessToken,
            Instant accessTokenExpiresAt,
            String refreshToken,
            Instant refreshTokenExpiresAt
    ) {

        static TokenResponse from(IssuedTokenPair tokenPair) {
            return new TokenResponse(
                    tokenPair.tokenType(),
                    tokenPair.accessToken(),
                    tokenPair.accessTokenExpiresAt(),
                    tokenPair.refreshToken(),
                    tokenPair.refreshTokenExpiresAt()
            );
        }
    }
}
