package com.polarishb.pabal.workspace.api.query.mapper;

import com.polarishb.pabal.security.authentication.PabalPrincipal;
import com.polarishb.pabal.workspace.api.query.http.response.WorkspaceResponse;
import com.polarishb.pabal.workspace.application.query.input.GetWorkspaceQuery;
import com.polarishb.pabal.workspace.application.query.output.WorkspaceDto;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class WorkspaceQueryMapper {

    public GetWorkspaceQuery toGetWorkspaceQuery(UUID workspaceId, Authentication authentication) {
        PabalPrincipal principal = extractPrincipal(authentication);
        return new GetWorkspaceQuery(principal.tenantId(), workspaceId);
    }

    public WorkspaceResponse toWorkspaceResponse(WorkspaceDto dto) {
        return new WorkspaceResponse(
                dto.workspaceId(),
                dto.tenantId(),
                dto.name(),
                dto.status(),
                dto.createdBy(),
                dto.createdAt(),
                dto.updatedAt()
        );
    }

    private PabalPrincipal extractPrincipal(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof PabalPrincipal principal) {
            return principal;
        }
        throw new AccessDeniedException("Missing authenticated principal");
    }
}
