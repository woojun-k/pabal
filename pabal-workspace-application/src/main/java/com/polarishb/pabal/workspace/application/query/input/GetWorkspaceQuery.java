package com.polarishb.pabal.workspace.application.query.input;

import com.polarishb.pabal.common.cqrs.Query;

import java.util.Objects;
import java.util.UUID;

public record GetWorkspaceQuery(
        UUID tenantId,
        UUID workspaceId
) implements Query {
    public GetWorkspaceQuery {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
    }
}
