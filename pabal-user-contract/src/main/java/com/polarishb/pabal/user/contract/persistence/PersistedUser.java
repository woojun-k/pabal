package com.polarishb.pabal.user.contract.persistence;

import com.polarishb.pabal.user.domain.model.User;

import java.util.Objects;

public record PersistedUser(
        User user,
        UserState state
) {
    public PersistedUser {
        Objects.requireNonNull(user, "user must not be null");
        Objects.requireNonNull(state, "state must not be null");
        if (!Objects.equals(user.getId(), state.id())) {
            throw new IllegalArgumentException("user id must match persisted state id");
        }
        if (!Objects.equals(user.getTenantId(), state.tenantId())) {
            throw new IllegalArgumentException("user tenantId must match persisted state tenantId");
        }
    }
}
