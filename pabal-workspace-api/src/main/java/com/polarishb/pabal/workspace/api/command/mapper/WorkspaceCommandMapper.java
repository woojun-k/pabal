package com.polarishb.pabal.workspace.api.command.mapper;

import com.polarishb.pabal.security.authentication.PabalPrincipal;
import com.polarishb.pabal.workspace.api.command.http.request.CreateWorkspaceRequest;
import com.polarishb.pabal.workspace.api.command.http.response.CreateWorkspaceResponse;
import com.polarishb.pabal.workspace.application.command.input.CreateWorkspaceCommand;
import com.polarishb.pabal.workspace.application.command.output.CreateWorkspaceResult;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceCommandMapper {

    public CreateWorkspaceCommand toCreateWorkspaceCommand(
            CreateWorkspaceRequest request,
            Authentication authentication
    ) {
        PabalPrincipal principal = extractPrincipal(authentication);
        return new CreateWorkspaceCommand(
                principal.tenantId(),
                principal.userId(),
                request.name()
        );
    }

    public CreateWorkspaceResponse toCreateWorkspaceResponse(CreateWorkspaceResult result) {
        return new CreateWorkspaceResponse(
                result.workspaceId(),
                result.tenantId(),
                result.name(),
                result.status(),
                result.ownerId(),
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
