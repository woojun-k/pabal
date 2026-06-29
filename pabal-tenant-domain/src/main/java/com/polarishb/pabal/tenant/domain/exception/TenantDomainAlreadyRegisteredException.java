package com.polarishb.pabal.tenant.domain.exception;

import com.polarishb.pabal.tenant.domain.exception.code.TenantErrorCode;
import com.polarishb.pabal.tenant.domain.model.vo.TenantDomainName;

public class TenantDomainAlreadyRegisteredException extends TenantException {

    public TenantDomainAlreadyRegisteredException(TenantDomainName domainName) {
        super(
                TenantErrorCode.TENANT_DOMAIN_ALREADY_REGISTERED,
                TenantErrorCode.TENANT_DOMAIN_ALREADY_REGISTERED.getMessage(),
                payload(entry("domainName", domainName == null ? null : domainName.value()))
        );
    }
}
