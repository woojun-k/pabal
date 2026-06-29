package com.polarishb.pabal.workspace.application.authorization;

import com.polarishb.pabal.common.authorization.FineGrainedPermission;

public enum WorkspacePermission implements FineGrainedPermission {
    WORKSPACE_CREATE("workspace:create"),
    WORKSPACE_READ("workspace:read"),
    WORKSPACE_UPDATE("workspace:update"),
    WORKSPACE_ARCHIVE("workspace:archive"),
    WORKSPACE_MEMBER_READ("workspace:member:read"),
    WORKSPACE_MEMBER_INVITE("workspace:member:invite"),
    WORKSPACE_MEMBER_ROLE_UPDATE("workspace:member:role:update"),
    WORKSPACE_MEMBER_REMOVE("workspace:member:remove");

    private final String value;

    WorkspacePermission(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }
}
