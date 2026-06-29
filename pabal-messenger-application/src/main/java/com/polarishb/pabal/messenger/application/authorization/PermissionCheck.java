package com.polarishb.pabal.messenger.application.authorization;

import com.polarishb.pabal.common.authorization.AuthorizationScope;
import com.polarishb.pabal.common.authorization.FineGrainedPermission;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record PermissionCheck(
        UUID tenantId,
        UUID requesterId,
        UUID workspaceId,
        UUID chatRoomId,
        FineGrainedPermission permission
) {

    public PermissionCheck {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(requesterId);
        Objects.requireNonNull(permission);
    }

    public List<AuthorizationScope> authorizationScopes() {
        List<AuthorizationScope> scopes = new ArrayList<>();
        scopes.add(AuthorizationScope.of("tenant", tenantId));
        scopes.add(AuthorizationScope.of("user", requesterId));
        if (workspaceId != null) {
            scopes.add(AuthorizationScope.of("workspace", workspaceId));
        }
        if (chatRoomId != null) {
            scopes.add(AuthorizationScope.of("room", chatRoomId));
        }
        return List.copyOf(scopes);
    }

    public static PermissionCheck tenant(
            UUID tenantId,
            UUID requesterId,
            FineGrainedPermission permission
    ) {
        return new PermissionCheck(tenantId, requesterId, null, null, permission);
    }

    public static PermissionCheck workspace(
            UUID tenantId,
            UUID requesterId,
            UUID workspaceId,
            FineGrainedPermission permission
    ) {
        Objects.requireNonNull(workspaceId);
        return new PermissionCheck(tenantId, requesterId, workspaceId, null, permission);
    }

    public static PermissionCheck room(
            UUID tenantId,
            UUID requesterId,
            UUID workspaceId,
            UUID chatRoomId,
            FineGrainedPermission permission
    ) {
        Objects.requireNonNull(chatRoomId);
        return new PermissionCheck(tenantId, requesterId, workspaceId, chatRoomId, permission);
    }
}
