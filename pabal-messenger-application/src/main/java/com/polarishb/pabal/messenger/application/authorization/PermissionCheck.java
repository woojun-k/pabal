package com.polarishb.pabal.messenger.application.authorization;

import java.util.Objects;
import java.util.UUID;

public record PermissionCheck(
        UUID tenantId,
        UUID requesterId,
        UUID workspaceId,
        UUID chatRoomId,
        MessengerPermission permission
) {

    public PermissionCheck {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(requesterId);
        Objects.requireNonNull(permission);
    }

    public static PermissionCheck workspace(
            UUID tenantId,
            UUID requesterId,
            UUID workspaceId,
            MessengerPermission permission
    ) {
        Objects.requireNonNull(workspaceId);
        return new PermissionCheck(tenantId, requesterId, workspaceId, null, permission);
    }

    public static PermissionCheck room(
            UUID tenantId,
            UUID requesterId,
            UUID workspaceId,
            UUID chatRoomId,
            MessengerPermission permission
    ) {
        Objects.requireNonNull(chatRoomId);
        return new PermissionCheck(tenantId, requesterId, workspaceId, chatRoomId, permission);
    }
}
