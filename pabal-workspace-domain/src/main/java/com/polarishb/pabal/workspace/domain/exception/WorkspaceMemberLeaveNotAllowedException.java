package com.polarishb.pabal.workspace.domain.exception;

import com.polarishb.pabal.workspace.domain.exception.code.WorkspaceErrorCode;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceMemberStatus;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceRole;

import java.util.UUID;

public class WorkspaceMemberLeaveNotAllowedException extends WorkspaceException {

    public WorkspaceMemberLeaveNotAllowedException(
            UUID memberId,
            WorkspaceRole role,
            WorkspaceMemberStatus status,
            String reason
    ) {
        super(
                WorkspaceErrorCode.WORKSPACE_MEMBER_LEAVE_NOT_ALLOWED,
                WorkspaceErrorCode.WORKSPACE_MEMBER_LEAVE_NOT_ALLOWED.getMessage(),
                payload(
                        entry("memberId", memberId),
                        entry("role", role),
                        entry("status", status),
                        entry("reason", reason)
                )
        );
    }
}
