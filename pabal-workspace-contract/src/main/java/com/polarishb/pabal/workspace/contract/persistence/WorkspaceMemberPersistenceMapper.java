package com.polarishb.pabal.workspace.contract.persistence;

import com.polarishb.pabal.workspace.domain.model.WorkspaceMember;

public final class WorkspaceMemberPersistenceMapper {

    private WorkspaceMemberPersistenceMapper() {
    }

    public static WorkspaceMember toDomain(WorkspaceMemberState state) {
        return WorkspaceMember.reconstitute(state.snapshot());
    }

    public static WorkspaceMemberState toState(WorkspaceMember member, Long version) {
        return new WorkspaceMemberState(member.snapshot(), version);
    }

    public static PersistedWorkspaceMember toPersisted(WorkspaceMemberState state) {
        return new PersistedWorkspaceMember(toDomain(state), state);
    }
}
