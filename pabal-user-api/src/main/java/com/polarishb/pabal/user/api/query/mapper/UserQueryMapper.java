package com.polarishb.pabal.user.api.query.mapper;

import com.polarishb.pabal.security.authentication.PabalPrincipal;
import com.polarishb.pabal.user.api.query.http.response.UserResponse;
import com.polarishb.pabal.user.application.query.input.GetUserQuery;
import com.polarishb.pabal.user.application.query.output.UserDto;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserQueryMapper {

    public GetUserQuery toGetMeQuery(Authentication authentication) {
        PabalPrincipal principal = extractPrincipal(authentication);
        return new GetUserQuery(principal.tenantId(), principal.userId());
    }

    public GetUserQuery toGetUserQuery(UUID userId, Authentication authentication) {
        PabalPrincipal principal = extractPrincipal(authentication);
        return new GetUserQuery(principal.tenantId(), userId);
    }

    public UserResponse toUserResponse(UserDto user) {
        return new UserResponse(
                user.userId(),
                user.tenantId(),
                user.name(),
                user.status(),
                user.createdAt(),
                user.updatedAt()
        );
    }

    private PabalPrincipal extractPrincipal(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof PabalPrincipal principal) {
            return principal;
        }
        throw new AccessDeniedException("Missing authenticated principal");
    }
}
