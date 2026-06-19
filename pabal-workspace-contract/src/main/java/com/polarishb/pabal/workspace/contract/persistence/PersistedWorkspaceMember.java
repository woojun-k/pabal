package com.polarishb.pabal.workspace.contract.persistence;

import com.polarishb.pabal.workspace.domain.model.WorkspaceMember;

import java.util.Objects;

public record PersistedWorkspaceMember(
        WorkspaceMember member,
        WorkspaceMemberState state
) {
    public PersistedWorkspaceMember {
        Objects.requireNonNull(member);
        Objects.requireNonNull(state);
    }
}
