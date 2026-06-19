package com.polarishb.pabal.workspace.api.query.http;

import com.polarishb.pabal.workspace.api.query.http.response.WorkspaceResponse;
import com.polarishb.pabal.workspace.api.query.mapper.WorkspaceQueryMapper;
import com.polarishb.pabal.workspace.application.query.handler.GetWorkspaceQueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WorkspaceQueryController {

    private final WorkspaceQueryMapper workspaceQueryMapper;
    private final GetWorkspaceQueryHandler getWorkspaceQueryHandler;

    @GetMapping("/workspaces/{workspaceId}")
    public WorkspaceResponse getWorkspace(
            @PathVariable UUID workspaceId,
            Authentication authentication
    ) {
        return workspaceQueryMapper.toWorkspaceResponse(
                getWorkspaceQueryHandler.handle(
                        workspaceQueryMapper.toGetWorkspaceQuery(workspaceId, authentication)
                )
        );
    }
}
