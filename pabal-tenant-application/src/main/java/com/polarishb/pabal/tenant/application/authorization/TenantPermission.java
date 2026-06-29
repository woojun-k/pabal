package com.polarishb.pabal.tenant.application.authorization;

import com.polarishb.pabal.common.authorization.FineGrainedPermission;

public enum TenantPermission implements FineGrainedPermission {
    TENANT_READ("tenant:read"),
    TENANT_UPDATE("tenant:update"),
    TENANT_DELETE("tenant:delete"),
    TENANT_MEMBER_READ("tenant:member:read"),
    TENANT_MEMBER_ROLE_ASSIGN("tenant:member:role:assign"),
    TENANT_MEMBER_ROLE_REVOKE("tenant:member:role:revoke");

    private final String value;

    TenantPermission(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }
}
