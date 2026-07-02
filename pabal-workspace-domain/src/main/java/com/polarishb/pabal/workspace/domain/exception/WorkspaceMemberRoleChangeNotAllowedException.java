package com.polarishb.pabal.workspace.domain.exception;

import com.polarishb.pabal.workspace.domain.exception.code.WorkspaceErrorCode;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceMemberStatus;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceRole;

import java.util.UUID;

public class WorkspaceMemberRoleChangeNotAllowedException extends WorkspaceException {

    public WorkspaceMemberRoleChangeNotAllowedException(
            UUID memberId,
            WorkspaceRole currentRole,
            WorkspaceRole newRole,
            WorkspaceMemberStatus status,
            String reason
    ) {
        super(
                WorkspaceErrorCode.WORKSPACE_MEMBER_ROLE_CHANGE_NOT_ALLOWED,
                WorkspaceErrorCode.WORKSPACE_MEMBER_ROLE_CHANGE_NOT_ALLOWED.getMessage(),
                payload(
                        entry("memberId", memberId),
                        entry("currentRole", currentRole),
                        entry("newRole", newRole),
                        entry("status", status),
                        entry("reason", reason)
                )
        );
    }
}
