package com.polarishb.pabal.workspace.contract.persistence;

import com.polarishb.pabal.workspace.domain.model.Workspace;

import java.util.Objects;

public record PersistedWorkspace(
        Workspace workspace,
        WorkspaceState state
) {
    public PersistedWorkspace {
        Objects.requireNonNull(workspace);
        Objects.requireNonNull(state);
    }
}
