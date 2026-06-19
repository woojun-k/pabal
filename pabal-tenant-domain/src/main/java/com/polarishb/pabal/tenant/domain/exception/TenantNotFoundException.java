package com.polarishb.pabal.tenant.domain.exception;

import com.polarishb.pabal.tenant.domain.exception.code.TenantErrorCode;

import java.util.UUID;

public class TenantNotFoundException extends TenantException {

    public TenantNotFoundException(UUID tenantId) {
        super(
                TenantErrorCode.TENANT_NOT_FOUND,
                TenantErrorCode.TENANT_NOT_FOUND.getMessage(),
                payload(entry("tenantId", tenantId))
        );
    }
}
