package com.polarishb.pabal.workspace.api.command.http.response;

import java.time.Instant;
import java.util.UUID;

public record CreateWorkspaceResponse(
        UUID workspaceId,
        UUID tenantId,
        String name,
        String status,
        UUID ownerId,
        Instant createdAt
) {
}
