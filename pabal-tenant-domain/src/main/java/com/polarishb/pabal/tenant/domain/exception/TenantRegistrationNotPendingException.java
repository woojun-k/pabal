package com.polarishb.pabal.tenant.domain.exception;

import com.polarishb.pabal.tenant.domain.exception.code.TenantErrorCode;
import com.polarishb.pabal.tenant.domain.model.type.TenantRegistrationStatus;

import java.util.UUID;

public class TenantRegistrationNotPendingException extends TenantException {

    public TenantRegistrationNotPendingException(UUID registrationId, TenantRegistrationStatus status) {
        super(
                TenantErrorCode.TENANT_REGISTRATION_NOT_PENDING,
                TenantErrorCode.TENANT_REGISTRATION_NOT_PENDING.getMessage(),
                payload(
                        entry("registrationId", registrationId),
                        entry("status", status == null ? null : status.name())
                )
        );
    }
}
