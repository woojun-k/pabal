package com.polarishb.pabal.workspace.application.command.output;

import java.time.Instant;
import java.util.UUID;

public record CreateWorkspaceResult(
        UUID workspaceId,
        UUID tenantId,
        String name,
        String status,
        UUID ownerId,
        Instant createdAt
) {
}
