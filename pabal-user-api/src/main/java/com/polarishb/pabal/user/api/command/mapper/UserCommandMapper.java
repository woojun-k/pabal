package com.polarishb.pabal.user.api.command.mapper;

import com.polarishb.pabal.security.authentication.PabalPrincipal;
import com.polarishb.pabal.user.api.command.http.request.CreateUserRequest;
import com.polarishb.pabal.user.api.command.http.response.CreateUserResponse;
import com.polarishb.pabal.user.application.command.input.CreateUserCommand;
import com.polarishb.pabal.user.application.command.output.CreateUserResult;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class UserCommandMapper {

    public CreateUserCommand toCreateUserCommand(CreateUserRequest request, Authentication authentication) {
        PabalPrincipal principal = extractPrincipal(authentication);
        return new CreateUserCommand(
                principal.userId(),
                principal.tenantId(),
                request.name()
        );
    }

    public CreateUserResponse toCreateUserResponse(CreateUserResult result) {
        return new CreateUserResponse(
                result.userId(),
                result.tenantId(),
                result.name(),
                result.status(),
                result.createdAt()
        );
    }

    private PabalPrincipal extractPrincipal(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof PabalPrincipal principal) {
            return principal;
        }
        throw new AccessDeniedException("Missing authenticated principal");
    }
}
