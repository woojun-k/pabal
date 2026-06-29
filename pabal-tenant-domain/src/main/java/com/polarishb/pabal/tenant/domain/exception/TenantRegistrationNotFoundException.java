package com.polarishb.pabal.tenant.domain.exception;

import com.polarishb.pabal.tenant.domain.exception.code.TenantErrorCode;

import java.util.UUID;

public class TenantRegistrationNotFoundException extends TenantException {

    public TenantRegistrationNotFoundException(UUID registrationId) {
        super(
                TenantErrorCode.TENANT_REGISTRATION_NOT_FOUND,
                TenantErrorCode.TENANT_REGISTRATION_NOT_FOUND.getMessage(),
                payload(entry("registrationId", registrationId))
        );
    }
}
