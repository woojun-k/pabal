package com.polarishb.pabal.workspace.domain.exception;

import com.polarishb.pabal.workspace.domain.exception.code.WorkspaceErrorCode;

import java.util.UUID;

public class WorkspaceNotFoundException extends WorkspaceException {

    public WorkspaceNotFoundException(UUID workspaceId) {
        super(
                WorkspaceErrorCode.WORKSPACE_NOT_FOUND,
                WorkspaceErrorCode.WORKSPACE_NOT_FOUND.getMessage(),
                payload(entry("workspaceId", workspaceId))
        );
    }
}
