package com.polarishb.pabal.workspace.contract.persistence;

import com.polarishb.pabal.workspace.domain.model.Workspace;

public final class WorkspacePersistenceMapper {

    private WorkspacePersistenceMapper() {
    }

    public static Workspace toDomain(WorkspaceState state) {
        return Workspace.reconstitute(state.snapshot());
    }

    public static WorkspaceState toState(Workspace workspace, Long version) {
        return new WorkspaceState(workspace.snapshot(), version);
    }

    public static PersistedWorkspace toPersisted(WorkspaceState state) {
        return new PersistedWorkspace(toDomain(state), state);
    }
}
