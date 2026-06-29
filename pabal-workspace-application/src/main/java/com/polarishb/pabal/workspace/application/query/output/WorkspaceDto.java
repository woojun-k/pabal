package com.polarishb.pabal.workspace.application.query.output;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceDto(
        UUID workspaceId,
        UUID tenantId,
        String name,
        String status,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
