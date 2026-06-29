package com.polarishb.pabal.user.contract.persistence;

import com.polarishb.pabal.user.domain.model.User;

public final class UserPersistenceMapper {

    private UserPersistenceMapper() {
    }

    public static User toDomain(UserState state) {
        return User.reconstitute(state.snapshot());
    }

    public static UserState toState(User user, Long version) {
        return new UserState(user.snapshot(), version);
    }

    public static PersistedUser toPersisted(UserState state) {
        return new PersistedUser(toDomain(state), state);
    }
}
