package com.polarishb.pabal.workspace.api.command.http;

import com.polarishb.pabal.workspace.api.command.http.request.CreateWorkspaceRequest;
import com.polarishb.pabal.workspace.api.command.http.response.CreateWorkspaceResponse;
import com.polarishb.pabal.workspace.api.command.mapper.WorkspaceCommandMapper;
import com.polarishb.pabal.workspace.application.command.handler.CreateWorkspaceCommandHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WorkspaceCommandController {

    private final WorkspaceCommandMapper workspaceCommandMapper;
    private final CreateWorkspaceCommandHandler createWorkspaceCommandHandler;

    @PostMapping("/workspaces")
    public CreateWorkspaceResponse createWorkspace(
            @Valid @RequestBody CreateWorkspaceRequest request,
            Authentication authentication
    ) {
        return workspaceCommandMapper.toCreateWorkspaceResponse(
                createWorkspaceCommandHandler.handle(
                        workspaceCommandMapper.toCreateWorkspaceCommand(request, authentication)
                )
        );
    }
}
