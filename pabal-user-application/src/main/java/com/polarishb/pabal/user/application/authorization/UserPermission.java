package com.polarishb.pabal.user.application.authorization;

import com.polarishb.pabal.common.authorization.FineGrainedPermission;

public enum UserPermission implements FineGrainedPermission {
    USER_CREATE("user:create"),
    USER_READ_SELF("user:read:self"),
    USER_READ_ALL("user:read:all"),
    USER_UPDATE_SELF("user:update:self"),
    USER_UPDATE_ALL("user:update:all"),
    USER_DISABLE("user:disable");

    private final String value;

    UserPermission(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }
}
