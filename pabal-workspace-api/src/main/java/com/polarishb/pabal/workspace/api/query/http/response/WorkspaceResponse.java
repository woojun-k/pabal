package com.polarishb.pabal.workspace.api.query.http.response;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceResponse(
        UUID workspaceId,
        UUID tenantId,
        String name,
        String status,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
