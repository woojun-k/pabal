package com.polarishb.pabal.tenant.api.command.mapper;

import com.polarishb.pabal.tenant.api.command.http.request.RequestTenantRegistrationRequest;
import com.polarishb.pabal.tenant.api.command.http.response.TenantRegistrationResponse;
import com.polarishb.pabal.tenant.api.command.http.response.VerifyTenantDomainResponse;
import com.polarishb.pabal.tenant.application.command.input.RenewTenantRegistrationTokenCommand;
import com.polarishb.pabal.tenant.application.command.input.RequestTenantRegistrationCommand;
import com.polarishb.pabal.tenant.application.command.input.VerifyTenantDomainCommand;
import com.polarishb.pabal.tenant.application.command.output.TenantRegistrationResult;
import com.polarishb.pabal.tenant.application.command.output.VerifyTenantDomainResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TenantRegistrationCommandMapper {

    public RequestTenantRegistrationCommand toRequestTenantRegistrationCommand(RequestTenantRegistrationRequest request) {
        return new RequestTenantRegistrationCommand(request.tenantName(), request.domainName());
    }

    public VerifyTenantDomainCommand toVerifyTenantDomainCommand(UUID registrationId) {
        return new VerifyTenantDomainCommand(registrationId);
    }

    public RenewTenantRegistrationTokenCommand toRenewTenantRegistrationTokenCommand(UUID registrationId) {
        return new RenewTenantRegistrationTokenCommand(registrationId);
    }

    public TenantRegistrationResponse toTenantRegistrationResponse(TenantRegistrationResult result) {
        return new TenantRegistrationResponse(
                result.registrationId(),
                result.tenantName(),
                result.domainName(),
                result.status(),
                result.verificationDnsName(),
                result.verificationTxtValue(),
                result.expiresAt(),
                result.createdAt()
        );
    }

    public VerifyTenantDomainResponse toVerifyTenantDomainResponse(VerifyTenantDomainResult result) {
        return new VerifyTenantDomainResponse(
                result.registrationId(),
                result.tenantId(),
                result.tenantName(),
                result.domainName(),
                result.status(),
                result.verifiedAt(),
                result.activatedAt()
        );
    }
}
