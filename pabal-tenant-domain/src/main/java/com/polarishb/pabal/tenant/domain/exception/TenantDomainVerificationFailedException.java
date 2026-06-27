package com.polarishb.pabal.tenant.domain.exception;

import com.polarishb.pabal.tenant.domain.exception.code.TenantErrorCode;
import com.polarishb.pabal.tenant.domain.model.vo.TenantDomainName;

public class TenantDomainVerificationFailedException extends TenantException {

    public TenantDomainVerificationFailedException(TenantDomainName domainName, String verificationDnsName) {
        super(
                TenantErrorCode.TENANT_DOMAIN_VERIFICATION_FAILED,
                TenantErrorCode.TENANT_DOMAIN_VERIFICATION_FAILED.getMessage(),
                payload(
                        entry("domainName", domainName == null ? null : domainName.value()),
                        entry("verificationDnsName", verificationDnsName)
                )
        );
    }
}
