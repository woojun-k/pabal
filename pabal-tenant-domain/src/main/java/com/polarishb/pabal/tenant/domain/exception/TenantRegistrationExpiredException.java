package com.polarishb.pabal.tenant.domain.exception;

import com.polarishb.pabal.tenant.domain.exception.code.TenantErrorCode;

import java.time.Instant;
import java.util.UUID;

public class TenantRegistrationExpiredException extends TenantException {

    public TenantRegistrationExpiredException(UUID registrationId, Instant expiresAt) {
        super(
                TenantErrorCode.TENANT_REGISTRATION_EXPIRED,
                TenantErrorCode.TENANT_REGISTRATION_EXPIRED.getMessage(),
                payload(
                        entry("registrationId", registrationId),
                        entry("expiresAt", expiresAt)
                )
        );
    }
}
